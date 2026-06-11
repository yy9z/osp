import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useUserStore } from '@/stores'

NProgress.configure({
  showSpinner: false,
  trickleSpeed: 200,
  minimum: 0.1
})

const Login = () => import('@/views/Login.vue')
const Register = () => import('@/views/Register.vue')
const AdminRegister = () => import('@/views/AdminRegister.vue')
const MainLayout = () => import('@/layouts/MainLayout.vue')
const Dashboard = () => import('@/views/Dashboard.vue')
const Agent = () => import('@/views/Agent.vue')
const SecondHand = () => import('@/views/SecondHand.vue')
const SecondHandDetail = () => import('@/views/SecondHandDetail.vue')
const SecondHandMy = () => import('@/views/SecondHandMy.vue')
const SecondHandAdmin = () => import('@/views/admin/SecondHandAdmin.vue')
const LostFound = () => import('@/views/LostFound.vue')
const LostFoundAdmin = () => import('@/views/admin/LostFoundAdmin.vue')
const Messages = () => import('@/views/Messages.vue')
const DormitoryInfo = () => import('@/views/dormitory/DormitoryInfo.vue')
const DormitoryRepair = () => import('@/views/dormitory/DormitoryRepair.vue')
const DormitoryAdmin = () => import('@/views/admin/DormitoryAdmin.vue')
const Navigation = () => import('@/views/Navigation.vue')
const Profile = () => import('@/views/user/Profile.vue')
const UserManage = () => import('@/views/admin/UserManage.vue')
const MessageBroadcast = () => import('@/views/admin/MessageBroadcast.vue')

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: Register,
    meta: { title: '注册', requiresAuth: false }
  },
  {
    path: '/register-admin',
    name: 'AdminRegister',
    component: AdminRegister,
    meta: { title: '管理员注册', requiresAuth: false }
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/',
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: Dashboard,
        meta: { title: '首页' }
      },
      {
        path: 'agent',
        name: 'Agent',
        component: Agent,
        meta: { title: '智能助手' }
      },
      {
        path: 'secondhand',
        name: 'SecondHand',
        component: SecondHand,
        meta: { title: '二手交易' }
      },
      {
        path: 'secondhand/my',
        name: 'SecondHandMy',
        component: SecondHandMy,
        meta: { title: '我的发布' }
      },
      {
        path: 'secondhand/:id',
        name: 'SecondHandDetail',
        component: SecondHandDetail,
        meta: { title: '商品详情' }
      },
      {
        path: 'lostfound',
        redirect: '/lostfound/lost-board',
        meta: { title: '失物招领' },
        children: [
          {
            path: 'lost-board',
            name: 'LostFoundLostBoard',
            component: LostFound,
            meta: { title: '寻物公告' }
          },
          {
            path: 'found-board',
            name: 'LostFoundFoundBoard',
            component: LostFound,
            meta: { title: '招领公告' }
          },
          {
            path: 'my-lost',
            name: 'LostFoundMyLost',
            component: LostFound,
            meta: { title: '我的失物' }
          },
          {
            path: 'my-found',
            name: 'LostFoundMyFound',
            component: LostFound,
            meta: { title: '我的招领' }
          }
        ]
      },
      {
        path: 'messages',
        name: 'Messages',
        component: Messages,
        meta: { title: '消息中心' }
      },
      {
        path: 'dormitory',
        redirect: '/dormitory/info',
        meta: { title: '宿舍管理' },
        children: [
          {
            path: 'info',
            name: 'DormitoryInfo',
            component: DormitoryInfo,
            meta: { title: '宿舍信息' }
          },
          {
            path: 'repair',
            name: 'DormitoryRepair',
            component: DormitoryRepair,
            meta: { title: '报修申请', role: 'STUDENT,TEACHER,ADMIN,DORM_MANAGER' }
          },
          {
            path: 'manage',
            name: 'DormitoryManage',
            component: DormitoryRepair,
            meta: { title: '报修管理', role: 'ADMIN,DORM_MANAGER' }
          },
          {
            path: 'assignment',
            name: 'DormitoryAssignment',
            component: DormitoryAdmin,
            meta: { title: '宿舍管理', role: 'ADMIN,DORM_MANAGER' }
          }
        ]
      },
      {
        path: 'navigation',
        name: 'Navigation',
        component: Navigation,
        meta: { title: '校园导航' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: Profile,
        meta: { title: '个人中心' }
      },
      {
        path: 'admin/secondhand',
        name: 'SecondHandAdmin',
        component: SecondHandAdmin,
        meta: { title: '二手交易审核', role: 'ADMIN' }
      },
      {
        path: 'admin/lostfound',
        name: 'LostFoundAdmin',
        component: LostFoundAdmin,
        meta: { title: '失物招领管理', role: 'ADMIN' }
      },
      {
        path: 'admin/message',
        name: 'MessageBroadcast',
        component: MessageBroadcast,
        meta: { title: '系统公告', role: 'ADMIN' }
      },
      {
        path: 'admin/users',
        name: 'UserManage',
        component: UserManage,
        meta: { title: '账号管理', role: 'ADMIN' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const WHITE_LIST = ['/login', '/register', '/register-admin']

router.beforeEach(async (to, from, next) => {
  NProgress.start()

  document.title = `${to.meta.title || '校园一站式平台'} - 高校校园一站式平台`

  const token = sessionStorage.getItem('token')
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth !== false)

  if (WHITE_LIST.includes(to.path)) {
    if (token) {
      const userStore = useUserStore()
      if (userStore.isAdmin) {
        next({ path: '/admin/secondhand' })
      } else {
        next({ path: '/' })
      }
      NProgress.done()
      return
    }
    next()
    return
  }

  if (!requiresAuth) {
    next()
    return
  }

  if (!token) {
    next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
    NProgress.done()
    return
  }

  const userStore = useUserStore()

  if (!userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch (error) {
      console.error('获取用户信息失败:', error)
      userStore.logout()
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
      NProgress.done()
      return
    }
  }

  if (to.meta.role) {
    const requiredRoles = to.meta.role.split(',')
    const userRole = userStore.role || ''
    if (!userRole || !requiredRoles.includes(userRole)) {
      next({ path: '/' })
      NProgress.done()
      return
    }
  }

  next()
})

router.afterEach(() => {
  NProgress.done()
})

router.onError(() => {
  NProgress.done()
})

export default router
