import cv2
import threading
import time
import os
import numpy as np
import pymysql        
import queue
from pathlib import Path
from datetime import datetime
from typing import Dict
from contextlib import asynccontextmanager

from fastapi import FastAPI, Query, HTTPException
from fastapi.responses import StreamingResponse, Response
from fastapi.middleware.cors import CORSMiddleware
from ultralytics import YOLO

import torch

# ==================== 全局配置 ====================
ROOT = Path(__file__).resolve().parent

# 写入 PID 供 Java 关闭管理
PID_FILE = ROOT / "logs" / "python.pid"
PID_FILE.parent.mkdir(parents=True, exist_ok=True)
PID_FILE.write_text(str(os.getpid()))

VIDEO_DIR = ROOT / "traffic_video"  
if not VIDEO_DIR.exists():
    VIDEO_DIR.mkdir(parents=True)

MODEL_PATH = str(ROOT / "yolo26n.pt")

# ==================== GPU 设备检测 ====================
print(f"[GPU] PyTorch version: {torch.__version__}")
print(f"[GPU] CUDA available: {torch.cuda.is_available()}")
if torch.cuda.is_available():
    print(f"[GPU] CUDA device count: {torch.cuda.device_count()}")
    for i in range(torch.cuda.device_count()):
        print(f"[GPU] Device {i}: {torch.cuda.get_device_name(i)}")
        props = torch.cuda.get_device_properties(i)
        print(f"[GPU]   VRAM: {props.total_memory / 1024**3:.1f} GB")
    print(f"[GPU] Current device: {torch.cuda.current_device()}")
else:
    print("[GPU] WARNING: CUDA NOT AVAILABLE! Will run on CPU.")
BASELINE_FLOW = 0      
SCALE_FACTOR = 1        
VEHICLE_CLASSES = [2, 3, 5, 7] # COCO: car, motorcycle, bus, truck

# ==================== MySQL 数据库配置 ====================
DB_HOST = "127.0.0.1"
DB_PORT = 3306
DB_USER = "root"
DB_PASS = "123456"
DB_NAME = "traffic_camera"  # 【请注意】修改为你实际的数据库名字！！！

db_queue = queue.Queue()

def db_writer_worker():
    """MySQL 后台持久化线程 - 增强报错版"""
    print(f"[Database] Attempting to connect to MySQL at {DB_HOST}...")
    
    conn = None
    try:
        conn = pymysql.connect(
            host=DB_HOST, port=DB_PORT, user=DB_USER, password=DB_PASS, 
            database=DB_NAME, charset="utf8mb4", autocommit=True
        )
        cursor = conn.cursor()
        print(f"[Database] ✅ MySQL Connected Successfully! Saving to {DB_NAME}") # 如果没看到这一行，说明没连上
    except Exception as e:
        print(f"[DB FATAL ERROR] ❌ 数据库连接失败: {e}")
        return # 连接失败直接退出线程

    while True:
        task = db_queue.get()
        if task is None: break
            
        camera_id, direction, start_time, car_count = task
        try:
            conn.ping()
            sql = """
                INSERT INTO traffic_flow_data (camera_id, direction, start_time, car_count)
                VALUES (%s, %s, %s, %s)
                ON DUPLICATE KEY UPDATE car_count = car_count + VALUES(car_count)
            """
            cursor.execute(sql, (camera_id, direction, start_time, car_count))
            print(f"[Database] 💾 Data Saved: Cam {camera_id} | Time {start_time} | Count {car_count}")
        except Exception as e:
            print(f"[DB Write Error] ❌ 插入失败: {e}")
        finally:
            db_queue.task_done()
    
    if conn: conn.close()


# ==================== 核心处理器类 ====================
class VideoProcessor:
    """处理单个视频流，支持独立模式切换与统计存库"""
    def __init__(self, filename: str, source_path: str, camera_id: int):
        self.filename = filename         # 逻辑名 (用于 URL 接口展示)
        self.source_path = source_path   # 物理路径或 rtsp 地址
        self.camera_id = camera_id       # 关联数据库真实的 camera_id
        
        self.latest_jpeg = None
        self.frame_version = 0
        self.condition = threading.Condition()
        self.is_running = True
        self.mode = 'yolo'
        self.output_width = 1280
        self.seen_vehicles = set()
        self.recent_events = [] 
        
        # 专供入库的自然分钟计数器
        self.current_minute_str = datetime.now().strftime("%Y-%m-%d %H:%M:00")
        self.current_minute_count = 0

    def draw_premium_hud(self, frame, mode: str, total_count: int, current_minute_count: int):
        # (这部分绘图逻辑完全保留，不用做任何修改)
        h, w, _ = frame.shape
        scale_ratio = h / 720.0
        w_ratio = w / 1280.0
        
        banner_h = max(30, int(70 * scale_ratio))  
        font_scale = max(0.4, 0.6 * scale_ratio)   
        font_thick_1 = max(1, int(1 * scale_ratio)) 
        font_thick_2 = max(1, int(2 * scale_ratio)) 
        line_thick = max(1, int(3 * scale_ratio))  
        
        text_y = int(banner_h * 0.65)
        
        overlay = frame.copy()
        cv2.rectangle(overlay, (0, 0), (w, banner_h), (20, 20, 20), -1)
        cv2.addWeighted(overlay, 0.65, frame, 0.35, 0, frame)
        
        border_color = (0, 255, 0) if mode == 'raw' else (0, 140, 255)
        cv2.line(frame, (0, banner_h), (w, banner_h), (80, 80, 80), max(1, int(1*scale_ratio)), cv2.LINE_AA)
        cv2.line(frame, (0, 0), (w, 0), border_color, line_thick, cv2.LINE_AA)
        
        cam_x = int(20 * w_ratio)
        cv2.putText(frame, f"CAM: {self.camera_id}", (cam_x, text_y), 
                    cv2.FONT_HERSHEY_SIMPLEX, font_scale, (200, 200, 200), font_thick_1)
        
        mode_text = f"MODE: {mode.upper()}"
        mode_x = int(200 * w_ratio)
        cv2.putText(frame, mode_text, (mode_x, text_y), 
                    cv2.FONT_HERSHEY_SIMPLEX, font_scale, border_color, font_thick_2)
        
        stats_text = f"1min: {current_minute_count} | Total: {total_count}"
        (text_w, text_h), _ = cv2.getTextSize(stats_text, cv2.FONT_HERSHEY_SIMPLEX, font_scale, font_thick_2)
        
        margin_right = int(20 * w_ratio)
        stats_x = w - text_w - margin_right
        
        cv2.putText(frame, stats_text, (stats_x, text_y), 
                    cv2.FONT_HERSHEY_SIMPLEX, font_scale, (255, 255, 255), font_thick_2)

    def run(self):
        print(f"[Init] Loading model for CAM ID {self.camera_id}...")
        local_model = YOLO(MODEL_PATH)
        
        # 显式将模型移到 NVIDIA 独显
        if torch.cuda.is_available():
            local_model.to('cuda:0')
            device_name = torch.cuda.get_device_name(0)
            # 验证模型参数确实在 GPU 上
            try:
                param_device = next(local_model.model.parameters()).device
                print(f"[GPU] CAM {self.camera_id} model on: {param_device} ({device_name})")
                print(f"[GPU] CAM {self.camera_id} VRAM allocated: {torch.cuda.memory_allocated(0) / 1024**2:.0f} MB")
            except Exception:
                pass
        else:
            print(f"[GPU] CAM {self.camera_id} WARNING: CUDA not available, using CPU")
        
        dummy_frame = np.zeros((320, 320, 3), dtype=np.uint8) 
        local_model.predict(dummy_frame, verbose=False, device=0)

        while self.is_running:
            cap = cv2.VideoCapture(self.source_path)
            if not cap.isOpened():
                time.sleep(5)
                continue

            source_str = str(self.source_path).lower()
            is_live_stream = source_str.startswith(('rtsp://', 'http://', 'rtmp://','https://')) or source_str.isdigit()
            if is_live_stream: cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)

            fps = cap.get(cv2.CAP_PROP_FPS)
            if not fps or fps == 0 or np.isnan(fps): fps = 25.0
            frame_delay = 1.0 / fps

            video_start_time = time.time()
            frame_counter = 0

            while self.is_running:
                success, frame = cap.read()
                if not success:
                    if not is_live_stream:
                        cap.set(cv2.CAP_PROP_POS_FRAMES, 0)
                        self.seen_vehicles.clear()
                        self.recent_events.clear() 
                        video_start_time = time.time()
                        frame_counter = 0
                    else:
                        break
                    continue
                
                frame_counter += 1

                # 每2帧跑一次YOLO，降低CPU/GPU负载，避免处理慢触发cap.grab()跳帧导致画面卡顿
                if frame_counter % 2 == 1:
                    results = local_model.track(
                        frame, persist=True, classes=VEHICLE_CLASSES,
                        verbose=False, device=0, imgsz=320, tracker="bytetrack.yaml"
                    )
                else:
                    results = None

                now = time.time()
                render_frame = frame.copy() if self.mode == 'raw' else frame

                now_dt = datetime.fromtimestamp(now)
                minute_str = now_dt.strftime("%Y-%m-%d %H:%M:00")
                
                # ====== 触发存库逻辑 ======
                if minute_str != self.current_minute_str:
                    final_count = BASELINE_FLOW + (self.current_minute_count * SCALE_FACTOR)
                    
                    # 【核心修改点】将提取到的 camera_id 传入队列！
                    # 因为纯识别没有划分方向，默认存 1(北向南)，若后续加入了越界检测线，可根据坐标偏移修改此处
                    direction = 1 
                    db_queue.put((self.camera_id, direction, self.current_minute_str, final_count))
                    
                    self.current_minute_str = minute_str
                    self.current_minute_count = 0
                # ==========================

                if results and results[0].boxes is not None:
                    boxes = results[0].boxes
                    xyxys = boxes.xyxy.cpu().numpy()
                    confs = boxes.conf.cpu().numpy()
                    ids = boxes.id.cpu().numpy() if boxes.id is not None else None

                    for i in range(len(xyxys)):
                        if ids is not None:
                            tid = int(ids[i])
                            if tid not in self.seen_vehicles:
                                self.seen_vehicles.add(tid)
                                self.recent_events.append(now)
                                self.current_minute_count += 1
                        else:
                            tid = -1

                        if self.mode == 'yolo':
                            x1, y1, x2, y2 = map(int, xyxys[i])
                            conf = confs[i]
                            cv2.rectangle(render_frame, (x1, y1), (x2, y2), (0, 140, 255), 2)
                            label = f"ID:{tid} {conf:.2f}"
                            cv2.putText(render_frame, label, (x1, max(10, y1 - 5)), 
                                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 140, 255), 2)

                self.recent_events = [ts for ts in self.recent_events if now - ts <= 60]
                curr_min = len(self.recent_events)

                self.draw_premium_hud(render_frame, self.mode, len(self.seen_vehicles), curr_min)

                ret, jpg = cv2.imencode('.jpg', render_frame, [cv2.IMWRITE_JPEG_QUALITY, 60])
                if ret:
                    jpg_bytes = jpg.tobytes()
                    with self.condition:
                        self.latest_jpeg = jpg_bytes
                        self.frame_version += 1
                        self.condition.notify_all()

                if not is_live_stream:
                    expected_time = video_start_time + frame_counter * frame_delay
                    current_time = time.time()
                    if current_time < expected_time:
                        time.sleep(expected_time - current_time)
                    else:
                        behind_seconds = current_time - expected_time
                        frames_to_skip = int(behind_seconds / frame_delay)
                        if frames_to_skip > 0:
                            for _ in range(frames_to_skip): cap.grab(); frame_counter += 1

            cap.release()


# ==================== 生命周期与 API 管理 ====================
processors: Dict[str, VideoProcessor] = {}

@asynccontextmanager
async def lifespan(app: FastAPI):
    """【核心修改】数据库驱动的启动模型"""
    # 1. 启动数据库写入后台线程
    db_thread = threading.Thread(target=db_writer_worker, daemon=True)
    db_thread.start()

    # 2. 从 MySQL 读取摄像头配置字典
    cameras = []
    try:
        conn = pymysql.connect(
            host=DB_HOST, port=DB_PORT, user=DB_USER, password=DB_PASS, database=DB_NAME, charset="utf8mb4"
        )
        cursor = conn.cursor(pymysql.cursors.DictCursor)
        cursor.execute("SELECT camera_id, video_url FROM traffic_camera WHERE status = '0' AND video_url IS NOT NULL")
        cameras = cursor.fetchall()
        conn.close()
    except Exception as e:
        print(f"[DB Error] 启动时无法查询交通监控设备表: {e}")

    # 3. 动态构建分析线程
    local_files = [f for f in os.listdir(VIDEO_DIR)] if VIDEO_DIR.exists() else []
    
    for cam in cameras:
        cam_id = cam['camera_id']
        video_url = cam['video_url']
        
        # 场景 A: 数据库存的是 RTSP 或 HTTP 直播流
        if video_url.startswith(('rtsp://', 'http://', 'rtmp://','https://')):
            filename = f"cam_{cam_id}_stream"
            source_path = video_url
            
        # 场景 B: 数据库存的是本地文件名（可能带路径前缀，如 /traffic_video/traffic.mp4）
        else:
            # 提取纯文件名
            video_filename = os.path.basename(video_url)
            if video_filename in local_files:
                filename = f"cam_{cam_id}_{video_filename}"  # 唯一 key，避免多摄像头共用同一文件时覆盖
                source_path = str(VIDEO_DIR / video_filename)
            else:
                print(f"[Warning] 设备 {cam_id} 的视频源 '{video_url}' 无法访问或文件不存在，已跳过。")
                continue

        # 启动关联了 camera_id 的线程
        p = VideoProcessor(filename=filename, source_path=source_path, camera_id=cam_id)
        processors[filename] = p
        t = threading.Thread(target=p.run, daemon=True)
        t.start()
        
    print(f"[System] Multi-stream service online: {list(processors.keys())}")
    
    yield  
    
    # 系统安全退出逻辑
    for p in processors.values():
        p.is_running = False
        final_count = BASELINE_FLOW + (p.current_minute_count * SCALE_FACTOR)
        db_queue.put((p.camera_id, 1, p.current_minute_str, final_count)) # direction = 1
    
    db_queue.put(None) 
    db_thread.join(timeout=3.0)

app = FastAPI(title="齐安智行 - 交通流统计与存库服务", version="3.1.0", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/streams")
def list_streams():
    return [
        {
            "filename": name,
            "camera_id": p.camera_id,
            "mode": p.mode,
            "video_url": f"http://localhost:8001/video_feed/{name}",
            "snapshot_url": f"http://localhost:8001/snapshot/{name}",
            "switch_url": f"http://localhost:8001/switch_mode?name={name}&mode=raw"
        } for name, p in processors.items()
    ]

@app.get("/switch_mode")
def switch_mode(
    name: str = Query(..., description="视频文件名/流名"), 
    mode: str = Query(..., description="raw 或 yolo")
):
    if name not in processors:
        raise HTTPException(status_code=404, detail="视频未找到")
    processors[name].mode = mode
    return {"status": "success", "video": name, "new_mode": mode}

@app.get("/snapshot/{name}")
def snapshot(name: str):
    """返回单帧 JPEG 快照（短连接），供 grid 预览用，不占用 MJPEG 持久连接"""
    if name not in processors:
        raise HTTPException(status_code=404, detail="Stream not found")
    p = processors[name]
    with p.condition:
        jpg = p.latest_jpeg
    if jpg is None:
        raise HTTPException(status_code=503, detail="No frame available yet")
    return Response(content=jpg, media_type="image/jpeg")


@app.get("/video_feed/{name}")
def video_feed(name: str):
    if name not in processors:
        raise HTTPException(status_code=404, detail="Stream not found")

    def generate_frames():
        p = processors[name]
        last_version = -1
        while p.is_running:
            with p.condition:
                p.condition.wait(timeout=0.5)
                if p.frame_version == last_version:
                    continue
                jpg = p.latest_jpeg
                last_version = p.frame_version
            if jpg is not None:
                yield (b'--frame\r\n'
                       b'Content-Type: image/jpeg\r\n\r\n' + jpg + b'\r\n')

    return StreamingResponse(generate_frames(), media_type="multipart/x-mixed-replace; boundary=frame")


@app.post("/reload")
def reload():
    """重新扫描数据库，同步处理器（新增/删除设备后调用）"""
    try:
        conn = pymysql.connect(host=DB_HOST, port=DB_PORT, user=DB_USER, password=DB_PASS,
                               database=DB_NAME, charset="utf8mb4")
        cursor = conn.cursor(pymysql.cursors.DictCursor)
        cursor.execute(
            "SELECT camera_id, video_url FROM traffic_camera WHERE status = '0' AND video_url IS NOT NULL")
        cameras = cursor.fetchall()
        conn.close()
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"DB query failed: {e}")

    db_ids = set()  # camera_ids currently in DB
    added = []
    local_files = [f for f in os.listdir(VIDEO_DIR)] if VIDEO_DIR.exists() else []

    for cam in cameras:
        cid = cam['camera_id']
        db_ids.add(cid)
        if any(p.camera_id == cid for p in processors.values()):
            continue  # already running

        video_url = cam['video_url']
        if video_url.startswith(('rtsp://', 'http://', 'rtmp://', 'https://')):
            filename = f"cam_{cid}_stream"
            source_path = video_url
        else:
            vfn = os.path.basename(video_url)
            if vfn not in local_files:
                continue
            filename = f"cam_{cid}_{vfn}"
            source_path = str(VIDEO_DIR / vfn)

        p = VideoProcessor(filename=filename, source_path=source_path, camera_id=cid)
        processors[filename] = p
        threading.Thread(target=p.run, daemon=True).start()
        added.append(filename)
        print(f"[Reload] Started processor for camera {cid}: {filename}")

    removed = []
    for name, p in list(processors.items()):
        if p.camera_id not in db_ids:
            p.is_running = False
            del processors[name]
            removed.append(name)
            print(f"[Reload] Stopped processor for camera {p.camera_id}: {name}")

    return {"status": "ok", "added": added, "removed": removed}


@app.get("/traffic_counts")
def traffic_counts():
    """返回所有摄像头的实时车辆计数，供前端做阈值预警"""
    return [
        {
            "camera_id": p.camera_id,
            "filename": name,
            "curr_min_count": len(p.recent_events),
            "mode": p.mode
        } for name, p in processors.items()
    ]


if __name__ == "__main__":
    import uvicorn
    print("[System] Starting High-Performance MySQL Analytics Server on port 8001...")
    uvicorn.run(app, host="0.0.0.0", port=8001, reload=False)