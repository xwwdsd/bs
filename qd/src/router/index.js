import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const adminMeta = { requireAuth: true, requireAdmin: true }

const routes = [
  { path: '/', name: 'Home', component: () => import('@/views/home/index.vue'), meta: { title: '首页' } },
  { path: '/login', name: 'Login', component: () => import('@/views/auth/login.vue'), meta: { title: '登录', public: true } },
  { path: '/register', name: 'Register', component: () => import('@/views/auth/register.vue'), meta: { title: '注册', public: true } },
  { path: '/reset-password', name: 'ResetPassword', component: () => import('@/views/auth/ResetPassword.vue'), meta: { title: '重置密码', public: true } },
  { path: '/items', name: 'Items', component: () => import('@/views/items/index.vue'), meta: { title: 'CS2 市场' } },
  { path: '/items/dota2', name: 'Dota2Items', component: () => import('@/views/items/dota2.vue'), meta: { title: 'DOTA2 市场' } },
  { path: '/items/order/:orderId', name: 'SellOrderDetail', component: () => import('@/views/items/detail.vue'), meta: { title: '在售详情' } },
  { path: '/items/:id', name: 'ItemDetail', component: () => import('@/views/items/detail.vue'), meta: { title: '饰品详情' } },
  { path: '/news', name: 'News', component: () => import('@/views/news/index.vue'), meta: { title: '资讯' } },
  { path: '/player-shows', name: 'PlayerShows', component: () => import('@/views/player-show/index.vue'), meta: { title: '玩家秀' } },
  { path: '/messages', name: 'Messages', component: () => import('@/views/messages/index.vue'), meta: { title: '消息中心', requireAuth: true } },
  {
    path: '/user',
    name: 'UserCenter',
    component: () => import('@/views/user/index.vue'),
    meta: { title: '个人中心', requireAuth: true },
    children: [
      { path: 'profile', name: 'UserProfile', component: () => import('@/views/user/profile.vue'), meta: { title: '账号设置' } },
      { path: 'messages', redirect: '/messages' },
      { path: 'favorites', name: 'UserFavorites', component: () => import('@/views/user/favorite.vue'), meta: { title: '我的收藏' } },
      { path: 'submissions', name: 'UserSubmissions', component: () => import('@/views/user/submission.vue'), meta: { title: '我的投稿' } },
      { path: 'wallet', name: 'UserWallet', component: () => import('@/views/user/wallet.vue'), meta: { title: '我的资金' } },
      { path: 'player-shows', name: 'UserPlayerShows', component: () => import('@/views/user/playerShow.vue'), meta: { title: '玩家秀管理' } }
    ]
  },
  { path: '/user/inventory', name: 'UserInventory', component: () => import('@/views/user/inventory.vue'), meta: { title: '我的库存', requireAuth: true } },
  { path: '/user/sell-orders', name: 'UserSellOrders', component: () => import('@/views/user/sell-orders.vue'), meta: { title: '我的出售', requireAuth: true } },
  { path: '/user/buy-orders', name: 'UserBuyOrders', component: () => import('@/views/user/buy-orders.vue'), meta: { title: '我的求购', requireAuth: true } },
  { path: '/user/orders', name: 'UserOrders', component: () => import('@/views/user/orders.vue'), meta: { title: '我的订单', requireAuth: true } },
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { title: '后台管理', ...adminMeta },
    redirect: '/admin/dashboard',
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/dashboard.vue'), meta: { title: '后台控制台', ...adminMeta } },
      { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/users.vue'), meta: { title: '用户管理', ...adminMeta } },
      { path: 'items', name: 'AdminItems', component: () => import('@/views/admin/items.vue'), meta: { title: '饰品管理', ...adminMeta } },
      { path: 'inventory', name: 'AdminInventory', component: () => import('@/views/admin/inventory.vue'), meta: { title: '库存管理', ...adminMeta } },
      { path: 'buy-orders', name: 'AdminBuyOrders', component: () => import('@/views/admin/buy-orders.vue'), meta: { title: '求购单管理', ...adminMeta } },
      { path: 'sell-orders', name: 'AdminSellOrders', component: () => import('@/views/admin/sell-orders.vue'), meta: { title: '出售单管理', ...adminMeta } },
      { path: 'trade-orders', name: 'AdminTradeOrders', component: () => import('@/views/admin/trade-orders.vue'), meta: { title: '交易订单管理', ...adminMeta } },
      { path: 'wallets', name: 'AdminWallets', component: () => import('@/views/admin/wallets.vue'), meta: { title: '钱包流水管理', ...adminMeta } },
      { path: 'withdrawals', name: 'AdminWithdrawals', component: () => import('@/views/admin/withdrawals.vue'), meta: { title: '提现管理', ...adminMeta } },
      { path: 'favorites', name: 'AdminFavorites', component: () => import('@/views/admin/favorites.vue'), meta: { title: '收藏管理', ...adminMeta } },
      { path: 'messages', name: 'AdminMessages', component: () => import('@/views/admin/messages.vue'), meta: { title: '消息管理', ...adminMeta } },
      { path: 'content', name: 'AdminContent', component: () => import('@/views/admin/content.vue'), meta: { title: '内容运营', ...adminMeta } },
      { path: 'player-shows', name: 'AdminPlayerShows', component: () => import('@/views/admin/player-shows.vue'), meta: { title: '玩家秀管理', ...adminMeta } },
      { path: 'sync', name: 'AdminSync', component: () => import('@/views/admin/sync.vue'), meta: { title: '同步维护', ...adminMeta } }
    ]
  },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/error/404.vue'), meta: { title: '页面不存在' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title || '饰品交易平台'
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
