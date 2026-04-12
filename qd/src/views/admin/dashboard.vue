<template>
  <div class="admin-dashboard">
    <aside class="sidebar">
      <div class="brand">
        <h1>后台管理</h1>
      </div>

      <div class="menu-group">
        <div class="menu-title">导航</div>
        <button type="button" class="menu-item" :class="{ active: activeTab === 'dashboard' }" @click="activeTab = 'dashboard'">
          <span class="menu-dot"></span>
          <span>控制台</span>
        </button>
        <button type="button" class="menu-item" :class="{ active: activeTab === 'sync' }" @click="activeTab = 'sync'">
          <span class="menu-dot"></span>
          <span>Steam 饰品同步</span>
        </button>
        <button type="button" class="menu-item" :class="{ active: activeTab === 'users' }" @click="activeTab = 'users'">
          <span class="menu-dot"></span>
          <span>用户管理</span>
        </button>
        <button type="button" class="menu-item" :class="{ active: activeTab === 'items' }" @click="activeTab = 'items'">
          <span class="menu-dot"></span>
          <span>饰品管理</span>
        </button>
        <button type="button" class="menu-item" :class="{ active: activeTab === 'orders' }" @click="activeTab = 'orders'">
          <span class="menu-dot"></span>
          <span>订单管理</span>
        </button>
        <button type="button" class="menu-item" :class="{ active: activeTab === 'news' }" @click="activeTab = 'news'">
          <span class="menu-dot"></span>
          <span>资讯管理</span>
        </button>
        <button type="button" class="menu-item" :class="{ active: activeTab === 'playerShows' }" @click="activeTab = 'playerShows'">
          <span class="menu-dot"></span>
          <span>玩家秀管理</span>
        </button>
        <button type="button" class="menu-item" :class="{ active: activeTab === 'banners' }" @click="activeTab = 'banners'">
          <span class="menu-dot"></span>
          <span>轮播图管理</span>
        </button>
      </div>
    </aside>

    <section class="main-panel">
      <!-- 控制台页面 -->
      <div v-if="activeTab === 'dashboard'" class="dashboard-visual">
        <section class="visual-hero">
          <div>
            <span class="visual-eyebrow">运营总览</span>
            <h2>控制台</h2>
            <p>交易、用户、饰品与同步任务</p>
          </div>
          <div class="visual-hero-meta">
            <span>最近刷新：{{ refreshedAt }}</span>
            <el-button @click="refreshAll" :loading="dashboardLoading">刷新数据</el-button>
          </div>
        </section>

        <section class="visual-stat-grid">
          <article v-for="card in summaryCards" :key="card.label" class="visual-stat-card">
            <span>{{ card.label }}</span>
            <strong>{{ card.value }}</strong>
            <small>{{ card.detail }}</small>
          </article>
        </section>

        <section class="visual-grid">
          <article class="visual-card visual-card-wide">
            <div class="visual-card-head">
              <div>
                <h3>交易趋势</h3>
                <span>近 7 天订单与成交额</span>
              </div>
              <span class="visual-tag">Trade</span>
            </div>
            <v-chart
              v-if="hasTradeTrendData"
              class="dashboard-chart"
              :option="tradeTrendOption"
              autoresize
            />
            <div v-else class="chart-empty">暂无趋势数据</div>
          </article>

          <article class="visual-card">
            <div class="visual-card-head">
              <div>
                <h3>订单状态</h3>
                <span>待处理、完成与其他</span>
              </div>
              <span class="visual-tag">Orders</span>
            </div>
            <v-chart
              v-if="hasOrderStatusData"
              class="dashboard-chart compact-chart"
              :option="orderStatusOption"
              autoresize
            />
            <div v-else class="chart-empty">暂无订单数据</div>
          </article>

          <article class="visual-card visual-card-wide">
            <div class="visual-card-head">
              <div>
                <h3>用户增长</h3>
                <span>近 7 天新增与累计</span>
              </div>
              <span class="visual-tag">Users</span>
            </div>
            <v-chart
              v-if="hasUserGrowthData"
              class="dashboard-chart"
              :option="userGrowthOption"
              autoresize
            />
            <div v-else class="chart-empty">暂无用户增长数据</div>
          </article>

          <article class="visual-card sync-visual-card">
            <div class="visual-card-head">
              <div>
                <h3>Steam 同步状态</h3>
                <span>{{ syncStatus.message || '暂无任务' }}</span>
              </div>
              <el-tag :type="syncTagType">{{ syncLabel }}</el-tag>
            </div>

            <el-progress
              :percentage="progressPercentage"
              :status="progressState"
              :stroke-width="14"
            />

            <div class="sync-visual-list">
              <div>
                <span>已同步页数</span>
                <strong>{{ syncStatus.syncedPages || 0 }} / {{ syncStatus.plannedPages || 0 }}</strong>
              </div>
              <div>
                <span>剩余页数</span>
                <strong>{{ syncStatus.remainingPages || 0 }}</strong>
              </div>
              <div>
                <span>处理饰品</span>
                <strong>{{ syncStatus.processedCount || 0 }} / {{ syncStatus.totalCount || 0 }}</strong>
              </div>
            </div>

            <div class="visual-actions">
              <el-button type="primary" @click="activeTab = 'sync'">进入同步中心</el-button>
              <el-button @click="refreshSyncStatus" :loading="syncLoading">刷新同步状态</el-button>
            </div>
          </article>
        </section>
      </div>

      <!-- 同步中心页面 -->
      <div v-else-if="activeTab === 'sync'">
        <section class="admin-section">
          <el-card class="sync-card">
            <div class="intro-grid">
              <el-alert
                title="同步说明"
                type="info"
                :closable="false"
                show-icon
              >
                <p>1. 每次最多同步 200 页，避免长任务占用过久。</p>
                <p>2. 如果总页数超过 200，任务会保留进度，你可以继续同步下一批。</p>
                <p>3. 页面会自动轮询任务状态，不需要再盯后端日志。</p>
              </el-alert>

              <div class="sync-actions">
                <el-button
                  type="primary"
                  size="large"
                  :loading="startingSync"
                  :disabled="startingSync || syncStatus.running"
                  @click="handleSync"
                >
                  {{ primaryButtonText }}
                </el-button>
                <el-button
                  size="large"
                  :disabled="syncStatus.running"
                  @click="refreshSyncStatus"
                >
                  刷新状态
                </el-button>
              </div>
            </div>

            <div class="status-panel">
              <div class="status-head">
                <div>
                  <div class="status-title">任务状态</div>
                  <div class="status-message">{{ syncStatus.message || '尚未开始同步' }}</div>
                </div>
                <el-tag :type="statusTagType">{{ statusLabel }}</el-tag>
              </div>

              <el-progress
                :percentage="progressPercentage"
                :status="progressState"
                :stroke-width="16"
              />

              <div class="stats-grid">
                <div class="stat-item">
                  <span>总饰品数</span>
                  <strong>{{ syncStatus.totalCount || 0 }}</strong>
                </div>
                <div class="stat-item">
                  <span>总页数</span>
                  <strong>{{ syncStatus.totalPages || 0 }}</strong>
                </div>
                <div class="stat-item">
                  <span>本轮计划页数</span>
                  <strong>{{ syncStatus.plannedPages || 0 }}</strong>
                </div>
                <div class="stat-item">
                  <span>已同步页数</span>
                  <strong>{{ syncStatus.syncedPages || 0 }}</strong>
                </div>
                <div class="stat-item">
                  <span>已处理饰品</span>
                  <strong>{{ syncStatus.processedCount || 0 }}</strong>
                </div>
                <div class="stat-item">
                  <span>当前页</span>
                  <strong>{{ syncStatus.currentPage || 0 }}</strong>
                </div>
                <div class="stat-item">
                  <span>剩余页数</span>
                  <strong>{{ syncStatus.remainingPages || 0 }}</strong>
                </div>
                <div class="stat-item">
                  <span>剩余饰品数</span>
                  <strong>{{ syncStatus.remainingItems || 0 }}</strong>
                </div>
                <div class="stat-item">
                  <span>新增</span>
                  <strong>{{ syncStatus.savedCount || 0 }}</strong>
                </div>
                <div class="stat-item">
                  <span>更新</span>
                  <strong>{{ syncStatus.updatedCount || 0 }}</strong>
                </div>
                <div class="stat-item">
                  <span>跳过</span>
                  <strong>{{ syncStatus.skippedCount || 0 }}</strong>
                </div>
                <div class="stat-item">
                  <span>下批次页范围</span>
                  <strong>{{ nextBatchRange }}</strong>
                </div>
                <div class="stat-item">
                  <span>每页数量</span>
                  <strong>{{ syncStatus.pageSize || 0 }}</strong>
                </div>
                <div class="stat-item">
                  <span>重试次数</span>
                  <strong>{{ syncStatus.retryCount || 0 }}</strong>
                </div>
                <div class="stat-item">
                  <span>上次状态码</span>
                  <strong>{{ syncStatus.lastHttpStatus ?? '-' }}</strong>
                </div>
              </div>

              <div class="meta-grid">
                <div>开始时间：{{ formatDateTime(syncStatus.startedAt) }}</div>
                <div>结束时间：{{ formatDateTime(syncStatus.finishedAt) }}</div>
                <div>耗时：{{ formatDuration(syncStatus.lastDurationSeconds) }}</div>
              </div>

              <el-alert
                v-if="syncStatus.phase === 'cooldown'"
                :title="cooldownMessage"
                type="warning"
                :closable="false"
                show-icon
                class="status-alert"
              />

              <el-alert
                v-if="syncStatus.capped"
                :title="cappedMessage"
                type="warning"
                :closable="false"
                show-icon
                class="status-alert"
              />

              <el-alert
                v-if="syncStatus.canContinue && !syncStatus.running"
                :title="continueMessage"
                type="success"
                :closable="false"
                show-icon
                class="status-alert"
              />

              <el-alert
                v-if="syncStatus.phase === 'failed' && syncStatus.error"
                :title="syncStatus.error"
                type="error"
                :closable="false"
                show-icon
                class="status-alert"
              />
            </div>
          </el-card>
        </section>

        <section class="admin-section">
          <h2>库存映射修复</h2>
          <p class="section-desc">修复 `user_inventory` 中缺失的 `item_id` 映射。</p>

          <el-card class="sync-card">
            <div class="fix-row">
              <el-alert
                title="适用场景"
                type="warning"
                :closable="false"
                show-icon
              >
                <p>用于修复历史库存数据中 `item_id` 为空或映射错误的记录。</p>
              </el-alert>

              <div class="sync-actions">
                <el-button
                  type="warning"
                  size="large"
                  :loading="fixing"
                  @click="handleFixItemIds"
                >
                  修复物品 ID 映射
                </el-button>
              </div>
            </div>

            <el-alert
              v-if="fixResult"
              :title="fixResult"
              type="success"
              :closable="false"
              show-icon
              class="status-alert"
            />
            <el-alert
              v-if="fixError"
              :title="fixError"
              type="error"
              :closable="false"
              show-icon
              class="status-alert"
            />
          </el-card>
        </section>
      </div>

      <!-- 其他管理页面占位 -->
      <div v-else-if="activeTab === 'users'" class="embedded-admin-page">
        <div class="page-header">
          <h1>用户管理</h1>
          <p>管理系统用户信息、权限和状态</p>
        </div>

        <el-card class="content-card">
          <div class="card-header">
            <div class="header-left">
              <el-input
                v-model="userSearchKeyword"
                placeholder="搜索用户名/邮箱"
                class="search-input"
                clearable
                @keyup.enter="fetchUsers"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </div>
            <div class="header-right">
              <el-button @click="fetchUsers" :loading="loading">刷新</el-button>
            </div>
          </div>

          <el-table :data="users" v-loading="loading" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="username" label="用户名" min-width="150" />
            <el-table-column prop="email" label="邮箱" min-width="200" />
            <el-table-column prop="role" label="角色" width="100">
              <template #default="{ row }">
                <el-tag :type="row.role === 'admin' ? 'danger' : 'info'">
                  {{ row.role === 'admin' ? '管理员' : '普通用户' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'warning'">
                  {{ row.status === 1 ? '正常' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="注册时间" width="180" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="handleEditUser(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="handleDeleteUser(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              background
              layout="prev, pager, next, sizes, total"
              :current-page="userPage"
              :page-size="userPageSize"
              :total="userTotal"
              @current-change="handleUserPageChange"
              @size-change="handleUserSizeChange"
            />
          </div>
        </el-card>
      </div>

      <div v-else-if="activeTab === 'items'" class="embedded-admin-page">
        <div class="page-header">
          <h1>饰品管理</h1>
          <p>管理饰品数据、分类和价格</p>
        </div>

        <el-card class="content-card">
          <div class="card-header">
            <div class="header-left">
              <el-input
                v-model="itemSearchKeyword"
                placeholder="搜索饰品名称"
                class="search-input"
                clearable
                @keyup.enter="fetchItems"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </div>
            <div class="header-right">
              <el-button @click="fetchItems" :loading="loading">刷新</el-button>
            </div>
          </div>

          <el-table :data="items" v-loading="loading" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="名称" min-width="250" />
            <el-table-column prop="category" label="分类" width="120" />
            <el-table-column prop="exterior" label="外观" width="100" />
            <el-table-column prop="price" label="价格" width="100">
              <template #default="{ row }">
                <span class="price-text">¥ {{ row.price }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">
                  {{ row.status === 1 ? '在售' : '下架' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="handleEditItem(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="handleDeleteItem(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              background
              layout="prev, pager, next, sizes, total"
              :current-page="itemPage"
              :page-size="itemPageSize"
              :total="itemTotal"
              @current-change="handleItemPageChange"
              @size-change="handleItemSizeChange"
            />
          </div>
        </el-card>
      </div>

      <div v-else-if="activeTab === 'orders'" class="embedded-admin-page">
        <div class="page-header">
          <h1>订单管理</h1>
          <p>管理订单记录和交易状态</p>
        </div>

        <el-card class="content-card">
          <div class="card-header">
            <div class="header-left">
              <el-input
                v-model="orderSearchKeyword"
                placeholder="搜索订单号/用户名"
                class="search-input"
                clearable
                @keyup.enter="fetchOrders"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </div>
            <div class="header-right">
              <el-button @click="fetchOrders" :loading="loading">刷新</el-button>
            </div>
          </div>

          <el-table :data="orders" v-loading="loading" style="width: 100%">
            <el-table-column prop="id" label="订单号" width="180" />
            <el-table-column prop="username" label="用户" width="120" />
            <el-table-column prop="itemName" label="商品" min-width="200" />
            <el-table-column prop="price" label="价格" width="100">
              <template #default="{ row }">
                <span class="price-text">¥ {{ row.price }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getOrderStatusType(row.status)">
                  {{ getOrderStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="下单时间" width="180" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="handleViewOrder(row)">详情</el-button>
                <el-button size="small" type="danger" @click="handleDeleteOrder(row)">取消</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              background
              layout="prev, pager, next, sizes, total"
              :current-page="orderPage"
              :page-size="orderPageSize"
              :total="orderTotal"
              @current-change="handleOrderPageChange"
              @size-change="handleOrderSizeChange"
            />
          </div>
        </el-card>
      </div>

      <div v-else-if="activeTab === 'news'" class="embedded-admin-page">
        <div class="page-header">
          <h1>资讯管理</h1>
          <p>管理资讯文章、审核和发布</p>
        </div>

        <el-card class="content-card">
          <div class="card-header">
            <div class="header-left">
              <el-input
                v-model="newsSearchKeyword"
                placeholder="搜索资讯标题"
                class="search-input"
                clearable
                @keyup.enter="fetchNews"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </div>
            <div class="header-right">
              <el-button type="primary" @click="handleAddNews">新增资讯</el-button>
              <el-button @click="fetchNews" :loading="loading">刷新</el-button>
            </div>
          </div>

          <el-table :data="newsList" v-loading="loading" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="title" label="标题" min-width="300" />
            <el-table-column prop="category" label="分类" width="100" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'warning'">
                  {{ row.status === 1 ? '已发布' : '待审核' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="viewCount" label="浏览量" width="80" />
            <el-table-column prop="createdAt" label="发布时间" width="180" />
            <el-table-column label="操作" width="250" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="handleEditNews(row)">编辑</el-button>
                <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="handleAuditNews(row)">
                  {{ row.status === 1 ? '下架' : '发布' }}
                </el-button>
                <el-button size="small" type="danger" @click="handleDeleteNews(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              background
              layout="prev, pager, next, sizes, total"
              :current-page="newsPage"
              :page-size="newsPageSize"
              :total="newsTotal"
              @current-change="handleNewsPageChange"
              @size-change="handleNewsSizeChange"
            />
          </div>
        </el-card>
      </div>

      <div v-else-if="activeTab === 'playerShows'" class="embedded-admin-page">
        <div class="page-header">
          <h1>玩家秀管理</h1>
          <p>管理玩家展示内容</p>
        </div>

        <el-card class="content-card">
          <div class="card-header">
            <div class="header-left">
              <el-input
                v-model="playerShowSearchKeyword"
                placeholder="搜索玩家昵称"
                class="search-input"
                clearable
                @keyup.enter="fetchPlayerShows"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </div>
            <div class="header-right">
              <el-button @click="fetchPlayerShows" :loading="loading">刷新</el-button>
            </div>
          </div>

          <el-table :data="playerShowsList" v-loading="loading" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="playerName" label="玩家昵称" width="150" />
            <el-table-column prop="itemName" label="展示饰品" min-width="200" />
            <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">
                  {{ row.status === 1 ? '展示中' : '未展示' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="180" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="handleTogglePlayerShow(row)">
                  {{ row.status === 1 ? '取消展示' : '展示' }}
                </el-button>
                <el-button size="small" type="danger" @click="handleDeletePlayerShow(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              background
              layout="prev, pager, next, sizes, total"
              :current-page="playerShowPage"
              :page-size="playerShowPageSize"
              :total="playerShowTotal"
              @current-change="handlePlayerShowPageChange"
              @size-change="handlePlayerShowSizeChange"
            />
          </div>
        </el-card>
      </div>

      <div v-else-if="activeTab === 'banners'" class="embedded-admin-page">
        <div class="page-header">
          <h1>轮播图管理</h1>
          <p>管理首页轮播广告图片</p>
        </div>

        <el-card class="content-card">
          <div class="card-header">
            <div class="header-left">
              <el-alert
                title="轮播图说明"
                type="info"
                :closable="false"
                show-icon
                style="width: 100%;"
              >
                <p>1. 轮播图将显示在首页顶部，建议尺寸：1920x400px</p>
                <p>2. 可以设置轮播图的标题、描述和跳转链接</p>
                <p>3. 支持拖拽排序，序号越小越靠前</p>
              </el-alert>
            </div>
            <div class="header-right">
              <el-button type="primary" @click="handleAddBanner">新增轮播图</el-button>
              <el-button @click="fetchBanners" :loading="loading">刷新</el-button>
            </div>
          </div>

          <el-table :data="bannersList" v-loading="loading" style="width: 100%" row-key="id" border>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="imageUrl" label="图片" width="200">
              <template #default="{ row }">
                <el-image
                  :src="row.imageUrl"
                  fit="cover"
                  style="width: 160px; height: 80px; border-radius: 4px;"
                  :preview-src-list="[row.imageUrl]"
                />
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="200" />
            <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
            <el-table-column prop="linkUrl" label="跳转链接" min-width="200" show-overflow-tooltip />
            <el-table-column prop="sortOrder" label="排序" width="80" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="250" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="handleEditBanner(row)">编辑</el-button>
                <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="handleToggleBanner(row)">
                  {{ row.status === 1 ? '禁用' : '启用' }}
                </el-button>
                <el-button size="small" type="danger" @click="handleDeleteBanner(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              background
              layout="prev, pager, next, sizes, total"
              :current-page="bannerPage"
              :page-size="bannerPageSize"
              :total="bannerTotal"
              @current-change="handleBannerPageChange"
              @size-change="handleBannerSizeChange"
            />
          </div>
        </el-card>
      </div>

      <!-- 轮播图编辑/新增对话框 -->
      <el-dialog
        v-model="bannerDialogVisible"
        :title="isEditBanner ? '编辑轮播图' : '新增轮播图'"
        width="600px"
        @close="resetBannerForm"
      >
        <el-form
          ref="bannerFormRef"
          :model="bannerForm"
          :rules="bannerRules"
          label-width="100px"
        >
          <el-form-item label="图片" prop="imageUrl">
            <el-upload
              class="upload-demo"
              action="/api/v1/file/upload"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleBannerUploadSuccess"
              :on-error="handleBannerUploadError"
              :before-upload="beforeBannerUpload"
              name="file"
            >
              <img v-if="bannerForm.imageUrl" :src="bannerForm.imageUrl" class="uploaded-img" style="width: 100%; height: 200px; object-fit: cover; border-radius: 4px;" />
              <el-icon v-else class="upload-icon" style="font-size: 48px; color: #8c939d;"><Plus /></el-icon>
            </el-upload>
          </el-form-item>
          <el-form-item label="标题" prop="title">
            <el-input v-model="bannerForm.title" placeholder="请输入标题" />
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input
              v-model="bannerForm.description"
              type="textarea"
              :rows="3"
              placeholder="请输入描述"
            />
          </el-form-item>
          <el-form-item label="跳转链接" prop="linkUrl">
            <el-input v-model="bannerForm.linkUrl" placeholder="请输入跳转链接（可选）" />
          </el-form-item>
          <el-form-item label="排序" prop="sortOrder">
            <el-input-number v-model="bannerForm.sortOrder" :min="0" :max="999" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-switch v-model="bannerForm.status" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="bannerDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveBanner" :loading="savingBanner">确定</el-button>
        </template>
      </el-dialog>

      <!-- 资讯编辑/新增对话框 -->
      <el-dialog
        v-model="newsDialogVisible"
        :title="isEditNews ? '编辑资讯' : '新增资讯'"
        width="700px"
        @close="resetNewsForm"
      >
        <el-form
          ref="newsFormRef"
          :model="newsForm"
          :rules="newsRules"
          label-width="100px"
        >
          <el-form-item label="标题" prop="title">
            <el-input v-model="newsForm.title" placeholder="请输入标题" />
          </el-form-item>
          <el-form-item label="分类" prop="category">
            <el-select v-model="newsForm.category" placeholder="请选择分类">
              <el-option label="官方公告" value="official" />
              <el-option label="游戏资讯" value="news" />
              <el-option label="活动信息" value="activity" />
              <el-option label="版本更新" value="update" />
            </el-select>
          </el-form-item>
          <el-form-item label="封面图" prop="coverImage">
            <el-upload
              class="upload-demo"
              action="/api/v1/file/upload"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleNewsUploadSuccess"
              :on-error="handleNewsUploadError"
              :before-upload="beforeNewsUpload"
              name="file"
            >
              <img v-if="newsForm.coverImage" :src="newsForm.coverImage" class="uploaded-img" style="width: 100%; height: 200px; object-fit: cover; border-radius: 4px;" />
              <el-icon v-else class="upload-icon" style="font-size: 48px; color: #8c939d;"><Plus /></el-icon>
            </el-upload>
          </el-form-item>
          <el-form-item label="内容" prop="content">
            <el-input
              v-model="newsForm.content"
              type="textarea"
              :rows="10"
              placeholder="请输入内容"
            />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-switch v-model="newsForm.status" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="newsDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveNews" :loading="savingNews">确定</el-button>
        </template>
      </el-dialog>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { useUserStore } from '@/stores/user'
import { getSteamSyncStatus, syncItemsFromSteam } from '@/api/item'
import { post } from '@/utils/request'
import { getStatistics, getTradeTrend, getUserGrowth, getUsers, deleteUser, getAllItems, deleteItem, getAllOrders, cancelOrder, getAllNews, createNews, updateNews, deleteNews, auditNews, getAllPlayerShows, deletePlayerShow } from '@/api/admin'
import { getAllBanners, createBanner, updateBanner, deleteBanner, updateBannerStatus } from '@/api/banner'

use([CanvasRenderer, BarChart, LineChart, PieChart, GridComponent, LegendComponent, TooltipComponent])

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('dashboard')
const loading = ref(false)
const syncLoading = ref(false)
const statistics = ref({})
const refreshedAt = ref('-')
const syncStatus = ref(createDefaultSyncStatus())
const dashboardChartsLoading = ref(false)
const tradeTrend = ref({
  dates: [],
  orderCounts: [],
  amounts: []
})
const userGrowth = ref({
  dates: [],
  newUsers: [],
  totalUsers: []
})

// 监听 Tab 切换，自动加载对应数据
watch(activeTab, (newTab) => {
  if (newTab === 'users') {
    fetchUsers()
  } else if (newTab === 'items') {
    fetchItems()
  } else if (newTab === 'orders') {
    fetchOrders()
  } else if (newTab === 'news') {
    fetchNews()
  } else if (newTab === 'playerShows') {
    fetchPlayerShows()
  } else if (newTab === 'banners') {
    fetchBanners()
  }
}, { immediate: false })

// 同步相关
const startingSync = ref(false)
const fixing = ref(false)
const fixResult = ref('')
const fixError = ref('')

// 用户管理相关
const userSearchKeyword = ref('')
const users = ref([])
const userPage = ref(1)
const userPageSize = ref(20)
const userTotal = ref(0)

// 饰品管理相关
const itemSearchKeyword = ref('')
const items = ref([])
const itemPage = ref(1)
const itemPageSize = ref(20)
const itemTotal = ref(0)

// 订单管理相关
const orderSearchKeyword = ref('')
const orders = ref([])
const orderPage = ref(1)
const orderPageSize = ref(20)
const orderTotal = ref(0)

// 轮播图管理相关
const bannersList = ref([])
const bannerPage = ref(1)
const bannerPageSize = ref(20)
const bannerTotal = ref(0)

// 轮播图对话框相关
const bannerDialogVisible = ref(false)
const isEditBanner = ref(false)
const bannerFormRef = ref(null)
const savingBanner = ref(false)
const bannerForm = ref({
  id: null,
  imageUrl: '',
  title: '',
  description: '',
  linkUrl: '',
  sortOrder: 0,
  status: 1
})

const bannerRules = {
  imageUrl: [{ required: true, message: '请上传图片', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }]
}

// 资讯管理相关
const newsSearchKeyword = ref('')
const newsList = ref([])
const newsPage = ref(1)
const newsPageSize = ref(20)
const newsTotal = ref(0)

// 资讯对话框相关
const newsDialogVisible = ref(false)
const isEditNews = ref(false)
const newsFormRef = ref(null)
const savingNews = ref(false)
const newsForm = ref({
  id: null,
  title: '',
  category: '',
  coverImage: '',
  content: '',
  status: 0
})

const newsRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  coverImage: [{ required: true, message: '请上传封面图', trigger: 'change' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

// 玩家秀管理相关
const playerShowSearchKeyword = ref('')
const playerShowsList = ref([])
const playerShowPage = ref(1)
const playerShowPageSize = ref(20)
const playerShowTotal = ref(0)

// 上传相关
const uploadHeaders = {
  Authorization: 'Bearer ' + userStore.accessToken
}

// 轮播图上传相关
const beforeBannerUpload = (file) => {
  const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPG) {
    ElMessage.error('上传图片只能是 JPG/PNG 格式!')
  }
  if (!isLt2M) {
    ElMessage.error('上传图片大小不能超过 2MB!')
  }
  return isJPG && isLt2M
}

const handleBannerUploadSuccess = (res) => {
  if (res.code === 200) {
    bannerForm.value.imageUrl = res.data
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error(res.message || '图片上传失败')
  }
}

const handleBannerUploadError = (err) => {
  ElMessage.error('上传请求失败，请检查网络或登录状态')
  console.error(err)
}

// 资讯上传相关
const beforeNewsUpload = (file) => {
  const isJPG = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJPG) {
    ElMessage.error('上传图片只能是 JPG/PNG 格式!')
  }
  if (!isLt2M) {
    ElMessage.error('上传图片大小不能超过 2MB!')
  }
  return isJPG && isLt2M
}

const handleNewsUploadSuccess = (res) => {
  if (res.code === 200) {
    newsForm.value.coverImage = res.data
    ElMessage.success('封面上传成功')
  } else {
    ElMessage.error(res.message || '封面上传失败')
  }
}

const handleNewsUploadError = (err) => {
  ElMessage.error('上传请求失败，请检查网络或登录状态')
  console.error(err)
}

let pollTimer = null

function createDefaultSyncStatus() {
  return {
    running: false,
    phase: 'idle',
    message: '',
    plannedPages: 0,
    syncedPages: 0,
    remainingPages: 0,
    nextStartPage: 0,
    nextEndPage: 0,
    totalCount: 0,
    // 详细同步状态
    totalCount: 0,
    totalPages: 0,
    currentPage: 0,
    processedCount: 0,
    savedCount: 0,
    updatedCount: 0,
    skippedCount: 0,
    remainingItems: 0,
    remainingPageRange: '',
    pageSize: 0,
    retryCount: 0,
    lastHttpStatus: null,
    cooldownUntil: null,
    autoRetrying: false,
    capped: false,
    canContinue: false,
    startedAt: null,
    finishedAt: null,
    lastDurationSeconds: 0,
    error: null
  }
}

const pageTitle = computed(() => {
  const titles = {
    dashboard: '控制台',
    sync: 'Steam 饰品同步',
    users: '用户管理',
    items: '饰品管理',
    orders: '订单管理'
  }
  return titles[activeTab.value] || '后台管理'
})

const pageDescription = computed(() => {
  const descriptions = {
    dashboard: '这里保留传统后台样式，统计、同步入口和系统状态集中展示。',
    sync: '按批次同步 Steam 饰品数据，并在页面上直接查看总量、已同步进度和剩余页数。',
    users: '管理系统用户信息、权限和状态。',
    items: '管理饰品数据、分类和价格。',
    orders: '管理订单记录和交易状态。'
  }
  return descriptions[activeTab.value] || '后台管理系统'
})

const syncTagType = computed(() => {
  if (syncStatus.value.phase === 'cooldown') return 'warning'
  if (syncStatus.value.running) return 'warning'
  if (syncStatus.value.phase === 'completed') return 'success'
  if (syncStatus.value.phase === 'failed') return 'danger'
  if (syncStatus.value.phase === 'partial') return 'info'
  return ''
})

const syncLabel = computed(() => {
  if (syncStatus.value.phase === 'cooldown') return '冷却中'
  if (syncStatus.value.running) return '进行中'
  if (syncStatus.value.phase === 'completed') return '已完成'
  if (syncStatus.value.phase === 'failed') return '失败'
  if (syncStatus.value.phase === 'partial') return '可继续'
  return '空闲'
})

const nextBatchRange = computed(() => {
  if (!syncStatus.value.nextStartPage || !syncStatus.value.nextEndPage) {
    return '-'
  }
  return `${syncStatus.value.nextStartPage} - ${syncStatus.value.nextEndPage}`
})

// 同步中心相关计算属性
const primaryButtonText = computed(() => {
  if (syncStatus.value.phase === 'cooldown') {
    return '限流冷却中'
  }
  if (syncStatus.value.running) {
    return '同步进行中'
  }
  if (syncStatus.value.canContinue) {
    return '继续同步下一批'
  }
  return '开始同步 Steam 饰品'
})

const cappedMessage = computed(() => {
  return `本轮最多同步 ${syncStatus.value.plannedPages || 0} / ${syncStatus.value.totalPages || 0} 页，未同步页范围：${syncStatus.value.remainingPageRange || '-'}`
})

const continueMessage = computed(() => {
  if (!syncStatus.value.canContinue) {
    return ''
  }
  return `当前已保留进度，可继续同步第 ${nextBatchRange.value} 页。`
})

const cooldownMessage = computed(() => {
  if (syncStatus.value.phase !== 'cooldown') {
    return ''
  }
  return syncStatus.value.message || 'Steam 限流中，系统会自动重试。'
})

const progressPercentage = computed(() => {
  const total = Number(syncStatus.value.totalCount) || 0
  const processed = Number(syncStatus.value.processedCount) || 0
  if (!total) {
    return syncStatus.value.phase === 'completed' ? 100 : 0
  }
  return Math.min(100, Math.round((processed / total) * 100))
})

const statusLabel = computed(() => {
  const phaseMap = {
    cooldown: '冷却中',
    idle: '未开始',
    syncing: '同步中',
    completed: '已完成',
    partial: '可继续',
    failed: '失败'
  }
  return phaseMap[syncStatus.value.phase] || '未知'
})

const statusTagType = computed(() => {
  if (syncStatus.value.phase === 'completed') return 'success'
  if (syncStatus.value.phase === 'partial') return 'warning'
  if (syncStatus.value.phase === 'failed') return 'danger'
  if (syncStatus.value.phase === 'cooldown') return 'warning'
  if (syncStatus.value.running) return 'warning'
  return 'info'
})

const progressState = computed(() => {
  if (syncStatus.value.phase === 'failed') return 'exception'
  if (syncStatus.value.phase === 'completed') return 'success'
  return undefined
})

const dashboardLoading = computed(() => {
  return loading.value || syncLoading.value || dashboardChartsLoading.value
})

const summaryCards = computed(() => [
  {
    label: '用户总数',
    value: formatNumber(statistics.value.totalUsers),
    detail: `活跃用户 ${formatNumber(statistics.value.activeUsers)}`
  },
  {
    label: '订单总数',
    value: formatNumber(statistics.value.totalOrders),
    detail: `今日订单 ${formatNumber(statistics.value.todayOrders)}`
  },
  {
    label: '交易总额',
    value: formatMoney(statistics.value.totalTradeAmount),
    detail: `今日成交 ${formatMoney(statistics.value.todayTradeAmount)}`
  },
  {
    label: '饰品总数',
    value: formatNumber(statistics.value.totalItems),
    detail: `同步饰品 ${formatNumber(syncStatus.value.totalCount || statistics.value.totalItems)}`
  }
])

const orderBreakdown = computed(() => {
  const total = Number(statistics.value.totalOrders) || 0
  const pending = Math.max(0, Number(statistics.value.pendingOrders) || 0)
  const completed = Math.max(0, Number(statistics.value.completedOrders) || 0)
  const other = Math.max(0, total - pending - completed)

  return [
    { name: '待处理订单', value: pending },
    { name: '已完成订单', value: completed },
    { name: '其他订单', value: other }
  ]
})

const hasTradeTrendData = computed(() => {
  return hasAnyNumber(tradeTrend.value.orderCounts) || hasAnyNumber(tradeTrend.value.amounts)
})

const hasUserGrowthData = computed(() => {
  return hasAnyNumber(userGrowth.value.newUsers) || hasAnyNumber(userGrowth.value.totalUsers)
})

const hasOrderStatusData = computed(() => {
  return orderBreakdown.value.some((item) => Number(item.value) > 0)
})

const chartTextStyle = {
  color: '#64748b',
  fontSize: 12
}

const chartAxisLine = {
  lineStyle: {
    color: '#dbe3ef'
  }
}

const chartSplitLine = {
  lineStyle: {
    color: '#eef2f7'
  }
}

const tradeTrendOption = computed(() => {
  const dates = Array.isArray(tradeTrend.value.dates) ? tradeTrend.value.dates : []
  const orderCounts = toNumberList(tradeTrend.value.orderCounts)
  const amounts = toNumberList(tradeTrend.value.amounts)

  return {
    color: ['#2563eb', '#f59e0b'],
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#0f172a',
      borderWidth: 0,
      textStyle: { color: '#ffffff' },
      formatter: formatTradeTooltip
    },
    legend: {
      bottom: 0,
      icon: 'circle',
      textStyle: chartTextStyle
    },
    grid: {
      left: 44,
      right: 54,
      top: 34,
      bottom: 48
    },
    xAxis: {
      type: 'category',
      data: dates,
      boundaryGap: false,
      axisLine: chartAxisLine,
      axisTick: { show: false },
      axisLabel: chartTextStyle
    },
    yAxis: [
      {
        type: 'value',
        name: '订单',
        nameTextStyle: chartTextStyle,
        axisLabel: chartTextStyle,
        splitLine: chartSplitLine
      },
      {
        type: 'value',
        name: '成交额',
        nameTextStyle: chartTextStyle,
        axisLabel: {
          ...chartTextStyle,
          formatter: (value) => `¥${value}`
        },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '订单数',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: orderCounts,
        areaStyle: {
          color: 'rgba(37, 99, 235, 0.12)'
        }
      },
      {
        name: '成交额',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        symbolSize: 6,
        data: amounts
      }
    ]
  }
})

const userGrowthOption = computed(() => {
  const dates = Array.isArray(userGrowth.value.dates) ? userGrowth.value.dates : []
  const newUsers = toNumberList(userGrowth.value.newUsers)
  const totalUsers = toNumberList(userGrowth.value.totalUsers)

  return {
    color: ['#0ea5e9', '#16a34a'],
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#0f172a',
      borderWidth: 0,
      textStyle: { color: '#ffffff' },
      formatter: formatNumberTooltip
    },
    legend: {
      bottom: 0,
      icon: 'circle',
      textStyle: chartTextStyle
    },
    grid: {
      left: 44,
      right: 36,
      top: 34,
      bottom: 48
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: chartAxisLine,
      axisTick: { show: false },
      axisLabel: chartTextStyle
    },
    yAxis: {
      type: 'value',
      axisLabel: chartTextStyle,
      splitLine: chartSplitLine
    },
    series: [
      {
        name: '新增用户',
        type: 'bar',
        barWidth: 16,
        data: newUsers,
        itemStyle: {
          borderRadius: [4, 4, 0, 0]
        }
      },
      {
        name: '累计用户',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: totalUsers
      }
    ]
  }
})

const orderStatusOption = computed(() => {
  return {
    color: ['#f59e0b', '#16a34a', '#64748b'],
    tooltip: {
      trigger: 'item',
      backgroundColor: '#0f172a',
      borderWidth: 0,
      textStyle: { color: '#ffffff' },
      formatter: (params) => `${params.marker}${params.name}: ${formatNumber(params.value)} (${params.percent}%)`
    },
    legend: {
      bottom: 0,
      icon: 'circle',
      textStyle: chartTextStyle
    },
    series: [
      {
        name: '订单状态',
        type: 'pie',
        radius: ['52%', '72%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        label: {
          color: '#334155',
          formatter: '{b}\n{c}'
        },
        labelLine: {
          lineStyle: {
            color: '#cbd5e1'
          }
        },
        data: orderBreakdown.value
      }
    ]
  }
})

const refreshStatistics = async () => {
  loading.value = true
  try {
    const res = await getStatistics()
    statistics.value = res || {}
    refreshedAt.value = new Date().toLocaleString('zh-CN')
  } catch (error) {
    ElMessage.error(error?.message || '获取统计数据失败')
  } finally {
    loading.value = false
  }
}

const refreshSyncStatus = async () => {
  syncLoading.value = true
  try {
    const res = await getSteamSyncStatus()
    syncStatus.value = { ...createDefaultSyncStatus(), ...(res || {}) }
  } catch (error) {
    ElMessage.error(error?.message || '获取同步状态失败')
  } finally {
    syncLoading.value = false
  }
}

const refreshDashboardCharts = async () => {
  dashboardChartsLoading.value = true
  try {
    const [tradeResult, userResult] = await Promise.allSettled([
      getTradeTrend(7),
      getUserGrowth(7)
    ])

    if (tradeResult.status === 'fulfilled') {
      tradeTrend.value = {
        dates: tradeResult.value?.dates || [],
        orderCounts: tradeResult.value?.orderCounts || [],
        amounts: tradeResult.value?.amounts || []
      }
    } else {
      tradeTrend.value = { dates: [], orderCounts: [], amounts: [] }
    }

    if (userResult.status === 'fulfilled') {
      userGrowth.value = {
        dates: userResult.value?.dates || [],
        newUsers: userResult.value?.newUsers || [],
        totalUsers: userResult.value?.totalUsers || []
      }
    } else {
      userGrowth.value = { dates: [], newUsers: [], totalUsers: [] }
    }

    if (tradeResult.status === 'rejected' || userResult.status === 'rejected') {
      ElMessage.error('获取趋势数据失败')
    }
  } finally {
    dashboardChartsLoading.value = false
  }
}

const refreshAll = async () => {
  await Promise.all([refreshStatistics(), refreshSyncStatus(), refreshDashboardCharts()])
}

const formatAmount = (value) => {
  return Number(value || 0).toFixed(2)
}

function formatMoney(value) {
  return `¥ ${formatAmount(value)}`
}

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function toNumberList(list) {
  if (!Array.isArray(list)) return []
  return list.map((value) => Number(value) || 0)
}

function hasAnyNumber(list) {
  return toNumberList(list).some((value) => value > 0)
}

function formatTradeTooltip(params = []) {
  const rows = params.map((item) => {
    const value = item.seriesName === '成交额' ? formatMoney(item.value) : formatNumber(item.value)
    return `${item.marker}${item.seriesName}: ${value}`
  })

  return [params[0]?.axisValue || '', ...rows].join('<br/>')
}

function formatNumberTooltip(params = []) {
  const rows = params.map((item) => {
    return `${item.marker}${item.seriesName}: ${formatNumber(item.value)}`
  })

  return [params[0]?.axisValue || '', ...rows].join('<br/>')
}

const logout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    userStore.logout()
    router.push('/login')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('退出登录失败')
    }
  }
}

// 同步中心方法
const startPolling = () => {
  stopPolling()
  pollTimer = window.setInterval(async () => {
    await refreshSyncStatus()
    if (!syncStatus.value.running) {
      stopPolling()
    }
  }, 3000)
}

const stopPolling = () => {
  if (pollTimer) {
    window.clearInterval(pollTimer)
    pollTimer = null
  }
}

const handleSync = async () => {
  const actionText = syncStatus.value.canContinue ? '继续同步下一批' : '开始同步 Steam 饰品'

  try {
    await ElMessageBox.confirm(
      `${actionText}后，页面会自动刷新任务状态。是否继续？`,
      '确认同步',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    startingSync.value = true
    await syncItemsFromSteam()
    await refreshSyncStatus()
    startPolling()
    ElMessage.success(`${actionText}已提交`)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '启动同步失败')
    }
  } finally {
    startingSync.value = false
  }
}

const handleFixItemIds = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要修复库存中的物品 ID 映射吗？',
      '确认修复',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    fixing.value = true
    fixResult.value = ''
    fixError.value = ''

    const result = await post('/v1/steam/admin/fix-null-item-ids')
    fixResult.value = result || '修复完成'
    ElMessage.success('修复任务执行完成')
  } catch (error) {
    if (error !== 'cancel') {
      fixError.value = error?.message || '修复失败'
      ElMessage.error(fixError.value)
    }
  } finally {
    fixing.value = false
  }
}

function formatDateTime(value) {
  if (!value) return '-'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value

  return date.toLocaleString('zh-CN', {
    hour12: false
  })
}

function formatDuration(totalSeconds) {
  const total = Number(totalSeconds) || 0
  if (total <= 0) return '-'

  const hours = Math.floor(total / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  const seconds = total % 60

  if (hours > 0) {
    return `${hours}小时 ${minutes}分 ${seconds}秒`
  }
  if (minutes > 0) {
    return `${minutes}分 ${seconds}秒`
  }
  return `${seconds}秒`
}

// ==================== 用户管理方法 ====================

const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await getUsers({ page: userPage.value, size: userPageSize.value, keyword: userSearchKeyword.value })
    users.value = res?.list || res || []
    userTotal.value = res?.total || users.value.length
  } catch (error) {
    ElMessage.error(error?.message || '获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleUserPageChange = (newPage) => {
  userPage.value = newPage
  fetchUsers()
}

const handleUserSizeChange = (newSize) => {
  userPageSize.value = newSize
  userPage.value = 1
  fetchUsers()
}

const handleEditUser = (row) => {
  ElMessage.info(`编辑用户：${row.username}`)
  // TODO: 打开编辑对话框
}

const handleDeleteUser = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除用户 "${row.username}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    fetchUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

// ==================== 饰品管理方法 ====================

const fetchItems = async () => {
  loading.value = true
  try {
    const res = await getAllItems()
    items.value = res || []
    itemTotal.value = items.value.length
  } catch (error) {
    ElMessage.error(error?.message || '获取饰品列表失败')
  } finally {
    loading.value = false
  }
}

const handleItemPageChange = (newPage) => {
  itemPage.value = newPage
  fetchItems()
}

const handleItemSizeChange = (newSize) => {
  itemPageSize.value = newSize
  itemPage.value = 1
  fetchItems()
}

const handleEditItem = (row) => {
  ElMessage.info(`编辑饰品：${row.name}`)
  // TODO: 打开编辑对话框
}

const handleDeleteItem = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除饰品 "${row.name}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteItem(row.id)
    ElMessage.success('删除成功')
    fetchItems()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

// ==================== 订单管理方法 ====================

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await getAllOrders()
    orders.value = res || []
    orderTotal.value = orders.value.length
  } catch (error) {
    ElMessage.error(error?.message || '获取订单列表失败')
  } finally {
    loading.value = false
  }
}

const handleOrderPageChange = (newPage) => {
  orderPage.value = newPage
  fetchOrders()
}

const handleOrderSizeChange = (newSize) => {
  orderPageSize.value = newSize
  orderPage.value = 1
  fetchOrders()
}

const handleViewOrder = (row) => {
  ElMessage.info(`查看订单详情：${row.id}`)
  // TODO: 打开订单详情对话框
}

const handleDeleteOrder = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要取消订单 "${row.id}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cancelOrder(row.id)
    ElMessage.success('取消成功')
    fetchOrders()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '取消失败')
    }
  }
}

const getOrderStatusType = (status) => {
  const typeMap = {
    0: 'warning',
    1: 'primary',
    2: 'success',
    3: 'success',
    4: 'info'
  }
  return typeMap[status] || 'info'
}

const getOrderStatusText = (status) => {
  const textMap = {
    0: '待支付',
    1: '待发货',
    2: '待收货',
    3: '已完成',
    4: '已取消'
  }
  return textMap[status] || status
}

// ==================== 资讯管理方法 ====================

const fetchNews = async () => {
  loading.value = true
  try {
    const res = await getAllNews()
    newsList.value = res || []
    newsTotal.value = newsList.value.length
  } catch (error) {
    ElMessage.error(error?.message || '获取资讯列表失败')
  } finally {
    loading.value = false
  }
}

const handleNewsPageChange = (newPage) => {
  newsPage.value = newPage
  fetchNews()
}

const handleNewsSizeChange = (newSize) => {
  newsPageSize.value = newSize
  newsPage.value = 1
  fetchNews()
}

const handleAddNews = () => {
  isEditNews.value = false
  newsForm.value = {
    id: null,
    title: '',
    category: '',
    coverImage: '',
    content: '',
    status: 0
  }
  newsDialogVisible.value = true
}

const handleEditNews = (row) => {
  isEditNews.value = true
  newsForm.value = {
    id: row.id,
    title: row.title || '',
    category: row.category || '',
    coverImage: row.coverImage || '',
    content: row.content || '',
    status: row.status !== undefined ? row.status : 0
  }
  newsDialogVisible.value = true
}

const resetNewsForm = () => {
  if (newsFormRef.value) {
    newsFormRef.value.resetFields()
  }
  newsForm.value = {
    id: null,
    title: '',
    category: '',
    coverImage: '',
    content: '',
    status: 0
  }
}

const handleSaveNews = async () => {
  if (!newsFormRef.value) return
  
  await newsFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    try {
      savingNews.value = true
      if (isEditNews.value) {
        await updateNews(newsForm.value.id, newsForm.value)
        ElMessage.success('更新资讯成功')
      } else {
        await createNews(newsForm.value)
        ElMessage.success('新增资讯成功')
      }
      newsDialogVisible.value = false
      fetchNews()
    } catch (error) {
      ElMessage.error(error?.message || '操作失败')
    } finally {
      savingNews.value = false
    }
  })
}

const handleAuditNews = async (row) => {
  try {
    const newStatus = row.status === 1 ? 0 : 1
    await auditNews(row.id, newStatus, '')
    ElMessage.success('操作成功')
    fetchNews()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '操作失败')
    }
  }
}

const handleDeleteNews = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除资讯 "${row.title}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteNews(row.id)
    ElMessage.success('删除成功')
    fetchNews()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

// ==================== 玩家秀管理方法 ====================

const fetchPlayerShows = async () => {
  loading.value = true
  try {
    const res = await getAllPlayerShows()
    playerShowsList.value = res || []
    playerShowTotal.value = playerShowsList.value.length
  } catch (error) {
    ElMessage.error(error?.message || '获取玩家秀列表失败')
  } finally {
    loading.value = false
  }
}

const handlePlayerShowPageChange = (newPage) => {
  playerShowPage.value = newPage
  fetchPlayerShows()
}

const handlePlayerShowSizeChange = (newSize) => {
  playerShowPageSize.value = newSize
  playerShowPage.value = 1
  fetchPlayerShows()
}

const handleTogglePlayerShow = async (row) => {
  try {
    const newStatus = row.status === 1 ? 0 : 1
    // TODO: 调用更新状态的 API
    ElMessage.success('操作成功')
    fetchPlayerShows()
  } catch (error) {
    ElMessage.error(error?.message || '操作失败')
  }
}

const handleDeletePlayerShow = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除玩家秀 "${row.playerName}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deletePlayerShow(row.id)
    ElMessage.success('删除成功')
    fetchPlayerShows()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

// ==================== 轮播图管理方法 ====================

const fetchBanners = async () => {
  loading.value = true
  try {
    const res = await getAllBanners()
    bannersList.value = res || []
    bannerTotal.value = bannersList.value.length
  } catch (error) {
    ElMessage.error(error?.message || '获取轮播图列表失败')
  } finally {
    loading.value = false
  }
}

const handleBannerPageChange = (newPage) => {
  bannerPage.value = newPage
  fetchBanners()
}

const handleBannerSizeChange = (newSize) => {
  bannerPageSize.value = newSize
  bannerPage.value = 1
  fetchBanners()
}

const handleAddBanner = () => {
  isEditBanner.value = false
  bannerForm.value = {
    id: null,
    imageUrl: '',
    title: '',
    description: '',
    linkUrl: '',
    sortOrder: 0,
    status: 1
  }
  bannerDialogVisible.value = true
}

const handleEditBanner = (row) => {
  isEditBanner.value = true
  bannerForm.value = {
    id: row.id,
    imageUrl: row.imageUrl || '',
    title: row.title || '',
    description: row.description || '',
    linkUrl: row.linkUrl || '',
    sortOrder: row.sortOrder || 0,
    status: row.status !== undefined ? row.status : 1
  }
  bannerDialogVisible.value = true
}

const resetBannerForm = () => {
  if (bannerFormRef.value) {
    bannerFormRef.value.resetFields()
  }
  bannerForm.value = {
    id: null,
    imageUrl: '',
    title: '',
    description: '',
    linkUrl: '',
    sortOrder: 0,
    status: 1
  }
}

const handleSaveBanner = async () => {
  if (!bannerFormRef.value) return
  
  await bannerFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    try {
      savingBanner.value = true
      if (isEditBanner.value) {
        await updateBanner(bannerForm.value)
        ElMessage.success('更新轮播图成功')
      } else {
        await createBanner(bannerForm.value)
        ElMessage.success('新增轮播图成功')
      }
      bannerDialogVisible.value = false
      fetchBanners()
    } catch (error) {
      ElMessage.error(error?.message || '操作失败')
    } finally {
      savingBanner.value = false
    }
  })
}

const handleToggleBanner = async (row) => {
  try {
    const newStatus = row.status === 1 ? 0 : 1
    await updateBannerStatus(row.id, newStatus)
    ElMessage.success('操作成功')
    fetchBanners()
  } catch (error) {
    ElMessage.error(error?.message || '操作失败')
  }
}

const handleDeleteBanner = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除轮播图 "${row.title}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteBanner(row.id)
    ElMessage.success('删除成功')
    fetchBanners()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

onMounted(() => {
  refreshAll()
})
</script>

<style scoped>
.admin-dashboard {
  display: grid;
  grid-template-columns: 240px 1fr;
  min-height: 100vh;
  background: #f3f5f8;
  color: #1f2937;
}

.sidebar {
  display: flex;
  flex-direction: column;
  gap: 28px;
  padding: 28px 20px;
  background: #1f2430;
  color: #d8dee9;
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand h1 {
  margin: 0 0 4px;
  color: #ffffff;
  font-size: 20px;
}

.menu-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.menu-title {
  color: rgba(216, 222, 233, 0.62);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: #d8dee9;
  cursor: pointer;
  text-align: left;
}

.menu-item.active,
.menu-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

.menu-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f0b321;
}



.main-panel {
  padding: 28px;
}

.dashboard-visual {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.visual-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  padding: 22px 24px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}

.visual-eyebrow {
  display: inline-block;
  margin-bottom: 8px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.visual-hero h2 {
  margin: 0;
  color: #0f172a;
  font-size: 28px;
}

.visual-hero p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
}

.visual-hero-meta {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  color: #64748b;
  font-size: 14px;
}

.visual-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.visual-stat-card,
.visual-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}

.visual-stat-card {
  min-width: 0;
  padding: 20px;
}

.visual-stat-card span,
.visual-stat-card small {
  display: block;
  color: #64748b;
}

.visual-stat-card span {
  margin-bottom: 10px;
  font-size: 14px;
}

.visual-stat-card strong {
  display: block;
  color: #0f172a;
  font-size: 30px;
  line-height: 1.2;
  word-break: break-word;
}

.visual-stat-card small {
  margin-top: 12px;
  font-size: 13px;
}

.visual-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(300px, 1fr);
  gap: 20px;
}

.visual-card {
  min-width: 0;
  padding: 20px;
}

.visual-card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.visual-card-head h3 {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
}

.visual-card-head > div > span {
  display: block;
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}

.visual-tag {
  flex: 0 0 auto;
  padding: 4px 10px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.dashboard-chart {
  width: 100%;
  height: 320px;
}

.compact-chart {
  height: 300px;
}

.chart-empty {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 320px;
  border: 1px dashed #dbe3ef;
  border-radius: 8px;
  background: #f8fafc;
  color: #64748b;
}

.sync-visual-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.sync-visual-list {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

.sync-visual-list > div {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f8fafc;
}

.sync-visual-list span {
  color: #64748b;
}

.sync-visual-list strong {
  color: #0f172a;
  text-align: right;
}

.visual-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: auto;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card,
.panel-card,
.table-panel {
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}

.stat-card {
  padding: 20px;
}

.stat-label {
  display: block;
  margin-bottom: 10px;
  color: #6b7280;
}

.stat-card strong {
  color: #111827;
  font-size: 30px;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.panel-card {
  padding: 20px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-head h3 {
  margin: 0;
  font-size: 18px;
}

.panel-tag {
  padding: 4px 10px;
  border-radius: 999px;
  background: #eef2ff;
  color: #4f46e5;
  font-size: 12px;
  font-weight: 600;
}

.overview-list,
.sync-summary {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.overview-item,
.sync-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafc;
}

.overview-item span,
.sync-row span {
  color: #6b7280;
}

.overview-item strong,
.sync-row strong {
  color: #111827;
  text-align: right;
}

.panel-actions {
  display: flex;
  gap: 12px;
  margin-top: 18px;
}

.table-panel {
  padding: 20px;
}

.note-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.note-card {
  padding: 16px;
  border-radius: 12px;
  background: #f8fafc;
}

.note-card h4 {
  margin: 0 0 10px;
  font-size: 16px;
}

.note-card p {
  margin: 0;
  color: #6b7280;
  line-height: 1.7;
}

/* 同步中心样式 */
.admin-section {
  margin-bottom: 24px;
}

.admin-section h2 {
  margin: 0 0 8px;
  color: #1f2937;
  font-size: 20px;
}

.section-desc {
  margin: 0 0 16px;
  color: #6b7280;
  font-size: 14px;
}

:deep(.sync-card) {
  border: 1px solid #e5e7eb;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}

.intro-grid,
.fix-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  align-items: start;
}

.sync-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.status-panel {
  margin-top: 20px;
}

.status-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.status-title {
  margin-bottom: 6px;
  color: #6b7280;
  font-size: 13px;
}

.status-message {
  color: #111827;
  font-size: 18px;
  font-weight: 600;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 20px;
}

.stat-item {
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #f9fafb;
}

.stat-item span {
  display: block;
  margin-bottom: 6px;
  color: #6b7280;
  font-size: 13px;
}

.stat-item strong {
  color: #111827;
  font-size: 22px;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
  color: #6b7280;
  font-size: 13px;
}

.status-alert {
  margin-top: 16px;
}

.placeholder-page {
  padding: 60px 20px;
  text-align: center;
}

.embedded-admin-page {
  padding: 0;
}

.embedded-admin-page .page-header {
  margin-bottom: 24px;
}

.embedded-admin-page .page-header h1 {
  margin: 0 0 8px;
  font-size: 28px;
  color: #1f2937;
}

.embedded-admin-page .page-header p {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}

.embedded-admin-page .content-card {
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}

.embedded-admin-page .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 16px;
}

.embedded-admin-page .header-left {
  flex: 1;
}

.embedded-admin-page .search-input {
  width: 300px;
}

.embedded-admin-page .header-right {
  display: flex;
  gap: 12px;
}

.embedded-admin-page .pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.embedded-admin-page .price-text {
  color: #f59e0b;
  font-weight: 600;
}

@media (max-width: 1100px) {
  .stats-grid,
  .content-grid,
  .note-grid {
    grid-template-columns: 1fr 1fr;
  }

  .visual-stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .visual-grid {
    grid-template-columns: 1fr;
  }

  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .meta-grid {
    grid-template-columns: 1fr;
  }

  .intro-grid,
  .fix-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 860px) {
  .admin-dashboard {
    grid-template-columns: 1fr;
  }

  .sidebar {
    gap: 20px;
  }

  .stats-grid,
  .content-grid,
  .note-grid {
    grid-template-columns: 1fr;
  }

  .visual-hero {
    flex-direction: column;
  }

  .visual-hero-meta {
    justify-content: flex-start;
  }

  .visual-stat-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-chart,
  .chart-empty {
    height: 280px;
  }

  .topbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .status-head {
    flex-direction: column;
  }
}
</style>
