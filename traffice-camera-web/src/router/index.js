// // src/router/index.js  【完整可用版，直接全选复制替换】
// import { createRouter, createWebHistory } from 'vue-router'
//
// const routes = [
//     // 登录页（保留，但不强制跳转）
//     { path: '/login', name: 'Login', component: () => import('@/views/Login.vue')},
//     { path: '/', redirect: '/traffic' },
//     { path: '/traffic', name:'TrafficCamera', component: () => import('@/views/TrafficCamera.vue') },
//     { path: '/video-wall', name:'VideoWall', component: () => import('@/views/videoWall.vue') },
//     { path: '/flow-analysis', name:'FlowAnalysis',component: () => import('@/views/Analysis.vue')},
// ]
//
// const router = createRouter({
//     history: createWebHistory(),
//     routes
// })
//
// // ✅ 关键：直接放行所有路由，彻底跳过登录校验
// router.beforeEach((to, from, next) => {
//     next() // 不做任何登录判断，直接通过
// })
//
// export default router

import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/views/Login.vue'
import TrafficCamera from '@/views/TrafficCamera.vue'
import Analysis from '@/views/Analysis.vue'
import VideoWall from "@/views/videoWall.vue";

const routes = [
    { path: '/login', component: Login },
    { path: '/traffic', component: TrafficCamera, meta: { roles: ['admin'] } },
    { path: '/flow-analysis', component: Analysis },
    { path: '/video-wall', component: VideoWall },
    {
        path: '/user-management',
        component: () => import('@/views/UserManagement.vue'),
        meta: { roles: ['admin'] }
    },
    { path: '/', redirect: '/video-wall' }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 全局路由守卫
router.beforeEach((to, from, next) => {
    const accessToken = localStorage.getItem('accessToken')
    const role = localStorage.getItem('role') || ''

    // 未登录 → 只能去 /login
    if (!accessToken && to.path !== '/login') {
        next('/login')
        return
    }

    // 路由限制了角色 → 校验
    if (to.meta.roles && !to.meta.roles.includes(role)) {
        next('/video-wall')  // 无权限时跳转到视频大屏
        return
    }

    next()
})

export default router