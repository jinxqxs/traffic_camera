import request from '@/utils/request'

// 查询列表
export function listCamera(query) {
    return request({
        url: '/traffic/camera/list',
        method: 'get',
        params: query
    })
}

// 查询详情
export function getCamera(cameraId) {
    return request({
        url: '/traffic/camera/' + cameraId,
        method: 'get'
    })
}

// 新增
export function addCamera(data) {
    return request({
        url: '/traffic/camera',
        method: 'post',
        data: data
    })
}

// 修改
export function updateCamera(data) {
    return request({
        url: '/traffic/camera',
        method: 'put',
        data: data
    })
}

// 删除
export function delCamera(cameraId) {
    return request({
        url: '/traffic/camera/' + cameraId,
        method: 'delete'
    })
}

// 视频大屏
export function getVideoList(query) {
    return request({
        url: '/traffic/camera/videoList',
        method: 'get',
        params: query
    })
}