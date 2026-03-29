import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/home/index.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/register.vue'),
    meta: { title: '注册', public: true }
  },
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: () => import('@/views/auth/ResetPassword.vue'),
    meta: { title: '重置密码', public: true }
  },
  {
    path: '/items',
    name: 'Items',
    component: () => import('@/views/items/index.vue'),
    meta: { title: '市场' }
  },
  {
    path: '/news',
    name: 'News',
    component: () => import('@/views/news/index.vue'),
    meta: { title: '资讯' }
  },
  {
    path: '/player-shows',
    name: 'PlayerShows',
    component: () => import('@/views/player-show/index.vue'),
    meta: { title: '玩家秀' }
  },
  {
    path: '/items/:id',
    name: 'ItemDetail',
    component: () => import('@/views/items/detail.vue'),
    meta: { title: '饰品详情' }
  },
  {
    path: '/user',
    name: 'UserCenter',
    component: () => import('@/views/user/index.vue'),
    meta: { title: '个人中心', requireAuth: true },
    children: [
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('@/views/user/profile.vue'),
        meta: { title: '账号设置' }
      },
      {
        path: 'messages',
        name: 'UserMessages',
        component: () => import('@/views/user/message.vue'),
        meta: { title: '消息中心' }
      },
      {
        path: 'favorites',
        name: 'UserFavorites',
        component: () => import('@/views/user/favorite.vue'),
        meta: { title: '我的收藏' }
      },
      {
        path: 'submissions',
        name: 'UserSubmissions',
        component: () => import('@/views/user/submission.vue'),
        meta: { title: '我的投稿' }
      },
      {
        path: 'wallet',
        name: 'UserWallet',
        component: () => import('@/views/user/wallet.vue'),
        meta: { title: '我的资金' }
      },
      {
        path: 'player-shows',
        name: 'UserPlayerShows',
        component: () => import('@/views/user/playerShow.vue'),
        meta: { title: '玩家秀管理' }
      }
    ]
  },
  {
    path: '/user/inventory',
    name: 'UserInventory',
    component: () => import('@/views/user/inventory.vue'),
    meta: { title: '我的库存', requireAuth: true }
  },
  {
    path: '/user/sell-orders',
    name: 'UserSellOrders',
    component: () => import('@/views/user/sell-orders.vue'),
    meta: { title: '我的出售', requireAuth: true }
  },
  {
    path: '/user/buy-orders',
    name: 'UserBuyOrders',
    component: () => import('@/views/user/buy-orders.vue'),
    meta: { title: '我的求购', requireAuth: true }
  },
  {
    path: '/user/orders',
    name: 'UserOrders',
    component: () => import('@/views/user/orders.vue'),
    meta: { title: '我的订单', requireAuth: true }
  },
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('@/views/admin/dashboard.vue'),
    meta: { title: '管理后台', requireAuth: true, requireAdmin: true }
  },
  {
    path: '/admin/sync',
    redirect: '/admin'
  },
  {
    path: '/admin/users',
    name: 'AdminUsers',
    component: () => import('@/views/admin/users.vue'),
    meta: { title: '用户管理', requireAuth: true, requireAdmin: true }
  },
  {
    path: '/admin/items',
    name: 'AdminItems',
    component: () => import('@/views/admin/items.vue'),
    meta: { title: '饰品管理', requireAuth: true, requireAdmin: true }
  },
  {
    path: '/admin/orders',
    name: 'AdminOrders',
    component: () => import('@/views/admin/orders.vue'),
    meta: { title: '订单管理', requireAuth: true, requireAdmin: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - CS2Trade` : 'CS2Trade'

  const userStore = useUserStore()

  if (to.meta.requireAuth && !userStore.isLoggedIn) {
    next('/login')
    return
  }

  if (to.meta.requireAdmin && !userStore.isAdmin) {
    next('/')
    return
  }

  if ((to.path === '/login' || to.path === '/register') && userStore.isLoggedIn) {
    next('/')
    return
  }

  next()
})

export default router
