import { get, post, put, del } from '@/utils/request'

const cleanParams = (params = {}) => Object.fromEntries(
  Object.entries(params).filter(([, value]) => value !== '' && value !== null && value !== undefined)
)

export const getStatistics = () => get('/v1/admin/statistics')
export const getTradeTrend = (days = 7) => get('/v1/admin/statistics/trade-trend', { days })
export const getUserGrowth = (days = 7) => get('/v1/admin/statistics/user-growth', { days })
export const rebuildMarketData = (itemId) => post('/v1/admin/statistics/rebuild-market-data', null, { params: { itemId } })

export const getUsers = (params = {}) => get('/v1/admin/users', cleanParams(params))
export const getUserById = (userId) => get(`/v1/admin/users/${userId}`)
export const getUserOverview = (userId) => get(`/v1/admin/users/${userId}/overview`)
export const updateUser = (userId, user) => put(`/v1/admin/users/${userId}`, user)
export const updateUserStatus = (userId, status) => put(`/v1/admin/users/${userId}/status`, null, { params: { status } })
export const deleteUser = (userId) => del(`/v1/admin/users/${userId}`)

export const getAllItems = (params = {}) => get('/v1/admin/items', cleanParams(params))
export const addItem = (item) => post('/v1/admin/items', item)
export const updateItem = (itemId, item) => put(`/v1/admin/items/${itemId}`, item)
export const deleteItem = (itemId) => del(`/v1/admin/items/${itemId}`)

export const getInventories = (params = {}) => get('/v1/admin/inventory', cleanParams(params))
export const getInventoryById = (inventoryId) => get(`/v1/admin/inventory/${inventoryId}`)
export const fixInventoryItemMapping = (inventoryId, itemId) => put(`/v1/admin/inventory/${inventoryId}/item`, null, { params: { itemId } })
export const cleanAbnormalInventories = () => del('/v1/admin/inventory/abnormal')

export const getBuyOrders = (params = {}) => get('/v1/admin/buy-orders', cleanParams(params))
export const getBuyOrderById = (orderId) => get(`/v1/admin/buy-orders/${orderId}`)
export const cancelBuyOrder = (orderId) => post(`/v1/admin/buy-orders/${orderId}/cancel`)
export const expireBuyOrders = () => post('/v1/admin/buy-orders/expire')

export const getSellOrders = (params = {}) => get('/v1/admin/sell-orders', cleanParams(params))
export const getSellOrderById = (orderId) => get(`/v1/admin/sell-orders/${orderId}`)
export const cancelSellOrder = (orderId) => post(`/v1/admin/sell-orders/${orderId}/cancel`)

export const getAllOrders = (params = {}) => get('/v1/admin/orders', cleanParams(params))
export const getOrdersByStatus = (status) => get('/v1/admin/orders/by-status', { status })
export const getTradeOrderById = (orderId) => get(`/v1/admin/orders/${orderId}`)
export const cancelOrder = (orderId) => post(`/v1/admin/orders/${orderId}/cancel`)

export const getWallets = (params = {}) => get('/v1/admin/wallets', cleanParams(params))
export const getWalletByUserId = (userId) => get(`/v1/admin/wallets/${userId}`)
export const getWalletTransactions = (params = {}) => get('/v1/admin/wallet-transactions', cleanParams(params))

export const getWithdrawals = (params = {}) => get('/v1/admin/withdrawals', cleanParams(params))
export const getWithdrawalById = (withdrawalId) => get(`/v1/admin/withdrawals/${withdrawalId}`)
export const auditWithdrawal = (withdrawalId, status, reason) => post(`/v1/admin/withdrawals/${withdrawalId}/audit`, null, { params: { status, reason } })

export const getFavorites = (params = {}) => get('/v1/admin/favorites', cleanParams(params))
export const deleteFavorite = (favoriteId) => del(`/v1/admin/favorites/${favoriteId}`)

export const getMessages = (params = {}) => get('/v1/admin/messages', cleanParams(params))
export const markMessageRead = (messageId) => post(`/v1/admin/messages/${messageId}/read`)
export const batchDeleteMessages = (ids) => post('/v1/admin/messages/batch-delete', ids)
export const sendSystemMessage = (message) => post('/v1/admin/messages/system', message)

export const getAllNews = () => get('/v1/admin/news')
export const createNews = (news) => post('/v1/admin/news', news)
export const updateNews = (newsId, news) => put(`/v1/admin/news/${newsId}`, news)
export const auditNews = (newsId, status, reason) => put(`/v1/admin/news/${newsId}/audit`, null, { params: { status, reason } })
export const deleteNews = (newsId) => del(`/v1/admin/news/${newsId}`)

export const getAllPlayerShows = (params = {}) => get('/v1/admin/player-shows', cleanParams(params))
export const deletePlayerShow = (showId) => del(`/v1/admin/player-shows/${showId}`)
export const getPlayerShowComments = (params = {}) => get('/v1/admin/player-show-comments', cleanParams(params))
export const deletePlayerShowComment = (commentId) => del(`/v1/admin/player-show-comments/${commentId}`)
export const getPlayerShowLikes = (params = {}) => get('/v1/admin/player-show-likes', cleanParams(params))

export const getSteamSyncTasks = (params = {}) => get('/v1/admin/sync/tasks', cleanParams(params))
export const getLatestSteamSyncTask = (taskType) => get('/v1/admin/sync/tasks/latest', { taskType })

export const cleanCancelledSellOrders = () => del('/v1/admin/clean/sell-orders/cancelled')
export const cleanCancelledTradeOrders = () => del('/v1/admin/clean/trade-orders/cancelled')
export const cleanExpiredSellOrders = () => del('/v1/admin/clean/sell-orders/expired')
export const cleanOldCompletedOrders = (days = 30) => del('/v1/admin/clean/trade-orders/old-completed', { days })
export const cleanSoldInventory = () => del('/v1/admin/clean/inventory/sold')
export const cleanAllUselessData = () => del('/v1/admin/clean/all')

export const getBanners = () => get('/v1/admin/banners')
export const addBanner = (banner) => post('/v1/admin/banners', banner)
export const updateBanner = (bannerId, banner) => put(`/v1/admin/banners/${bannerId}`, banner)
export const deleteBanner = (bannerId) => del(`/v1/admin/banners/${bannerId}`)
export const updateBannerStatus = (bannerId, status) => put(`/v1/admin/banners/${bannerId}/status`, null, { params: { status } })
export const updateBannerSort = (bannerId, sortOrder) => put(`/v1/admin/banners/${bannerId}/sort`, null, { params: { sortOrder } })
