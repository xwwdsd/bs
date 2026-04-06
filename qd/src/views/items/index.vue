<template>
  <div class="market-page">
    <SiteHeader active="market" @auth-click="showLoginModal = true" />

    <LoginModal v-model="showLoginModal" />

    <main class="main-content">
      <!-- 分类导航区域 -->
      <section class="category-section">
        <div class="category-container">
          <div class="category-row">
            <div class="category-item" @click="categoryFilter = ''; subCategory = ''; handleSearch()">
              <div class="category-icon">
                <el-icon><Grid /></el-icon>
              </div>
              <span class="category-name">全部</span>
            </div>
            
            <div class="category-item-wrapper">
              <div class="category-item" :class="{ active: categoryFilter === 'knife' }">
                <div class="category-icon">
                  <el-icon><Aim /></el-icon>
                </div>
                <span class="category-name">匕首</span>
                <el-icon class="arrow-icon"><ArrowDown /></el-icon>
              </div>
              <div class="category-dropdown">
                <div class="dropdown-item" :class="{ active: subCategory === '' }" @click="subCategory = ''; categoryFilter = 'knife'; handleSearch()">全部匕首</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'butterfly' }" @click="subCategory = 'butterfly'; categoryFilter = 'knife'; handleSearch()">蝴蝶刀</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'm9' }" @click="subCategory = 'm9'; categoryFilter = 'knife'; handleSearch()">M9 刺刀</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'claw' }" @click="subCategory = 'claw'; categoryFilter = 'knife'; handleSearch()">爪子刀</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'karambit' }" @click="subCategory = 'karambit'; categoryFilter = 'knife'; handleSearch()">廓尔喀刀</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'skeleton' }" @click="subCategory = 'skeleton'; categoryFilter = 'knife'; handleSearch()">骷髅匕首</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'bayonet' }" @click="subCategory = 'bayonet'; categoryFilter = 'knife'; handleSearch()">刺刀</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'serrated' }" @click="subCategory = 'serrated'; categoryFilter = 'knife'; handleSearch()">锯齿爪刀</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'nomad' }" @click="subCategory = 'nomad'; categoryFilter = 'knife'; handleSearch()">流浪者匕首</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'flip' }" @click="subCategory = 'flip'; categoryFilter = 'knife'; handleSearch()">折叠刀</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'short' }" @click="subCategory = 'short'; categoryFilter = 'knife'; handleSearch()">短剑</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'gut' }" @click="subCategory = 'gut'; categoryFilter = 'knife'; handleSearch()">海豹短刀</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'bear' }" @click="subCategory = 'bear'; categoryFilter = 'knife'; handleSearch()">熊刀</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'huntsman' }" @click="subCategory = 'huntsman'; categoryFilter = 'knife'; handleSearch()">猎杀者匕首</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'cord' }" @click="subCategory = 'cord'; categoryFilter = 'knife'; handleSearch()">系绳匕首</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'survival' }" @click="subCategory = 'survival'; categoryFilter = 'knife'; handleSearch()">求生匕首</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'bowie' }" @click="subCategory = 'bowie'; categoryFilter = 'knife'; handleSearch()">弯刀</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'fangs' }" @click="subCategory = 'fangs'; categoryFilter = 'knife'; handleSearch()">暗影双匕</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'push' }" @click="subCategory = 'push'; categoryFilter = 'knife'; handleSearch()">鲍伊猎刀</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'gut-knife' }" @click="subCategory = 'gut-knife'; categoryFilter = 'knife'; handleSearch()">穿肠刀</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'navaja' }" @click="subCategory = 'navaja'; categoryFilter = 'knife'; handleSearch()">折刀</div>
              </div>
            </div>
            
            <div class="category-item-wrapper">
              <div class="category-item" :class="{ active: categoryFilter === 'glove' }">
                <div class="category-icon">
                  <el-icon><Aim /></el-icon>
                </div>
                <span class="category-name">手套</span>
                <el-icon class="arrow-icon"><ArrowDown /></el-icon>
              </div>
              <div class="category-dropdown">
                <div class="dropdown-item" :class="{ active: subCategory === '' }" @click="subCategory = ''; categoryFilter = 'glove'; handleSearch()">全部手套</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'sport' }" @click="subCategory = 'sport'; categoryFilter = 'glove'; handleSearch()">运动手套</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'specialist' }" @click="subCategory = 'specialist'; categoryFilter = 'glove'; handleSearch()">专业手套</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'moto' }" @click="subCategory = 'moto'; categoryFilter = 'glove'; handleSearch()">摩托手套</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'driver' }" @click="subCategory = 'driver'; categoryFilter = 'glove'; handleSearch()">驾驶手套</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'hand' }" @click="subCategory = 'hand'; categoryFilter = 'glove'; handleSearch()">手部束带</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'hydra' }" @click="subCategory = 'hydra'; categoryFilter = 'glove'; handleSearch()">狂牙手套</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'nine' }" @click="subCategory = 'nine'; categoryFilter = 'glove'; handleSearch()">九头蛇手套</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'bloodhound' }" @click="subCategory = 'bloodhound'; categoryFilter = 'glove'; handleSearch()">血猎手套</div>
              </div>
            </div>
            
            <div class="category-item-wrapper">
              <div class="category-item" :class="{ active: categoryFilter === 'rifle' }">
                <div class="category-icon">
                  <el-icon><Aim /></el-icon>
                </div>
                <span class="category-name">步枪</span>
                <el-icon class="arrow-icon"><ArrowDown /></el-icon>
              </div>
              <div class="category-dropdown">
                <div class="dropdown-item" :class="{ active: subCategory === '' }" @click="subCategory = ''; categoryFilter = 'rifle'; handleSearch()">全部步枪</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'ak47' }" @click="subCategory = 'ak47'; categoryFilter = 'rifle'; handleSearch()">AK-47</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'm4a4' }" @click="subCategory = 'm4a4'; categoryFilter = 'rifle'; handleSearch()">M4A4</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'm4a1s' }" @click="subCategory = 'm4a1s'; categoryFilter = 'rifle'; handleSearch()">M4A1 消音版</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'aug' }" @click="subCategory = 'aug'; categoryFilter = 'rifle'; handleSearch()">AUG</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'famas' }" @click="subCategory = 'famas'; categoryFilter = 'rifle'; handleSearch()">法玛斯</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'galil' }" @click="subCategory = 'galil'; categoryFilter = 'rifle'; handleSearch()">加利利 AR</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'sg553' }" @click="subCategory = 'sg553'; categoryFilter = 'rifle'; handleSearch()">SG 553</div>
              </div>
            </div>

            <div class="category-item-wrapper">
              <div class="category-item" :class="{ active: categoryFilter === 'sniper_rifle' }">
                <div class="category-icon">
                  <el-icon><Aim /></el-icon>
                </div>
                <span class="category-name">狙击步枪</span>
                <el-icon class="arrow-icon"><ArrowDown /></el-icon>
              </div>
              <div class="category-dropdown">
                <div class="dropdown-item" :class="{ active: subCategory === '' }" @click="subCategory = ''; categoryFilter = 'sniper_rifle'; handleSearch()">全部狙击步枪</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'awp' }" @click="subCategory = 'awp'; categoryFilter = 'sniper_rifle'; handleSearch()">AWP</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'ssg08' }" @click="subCategory = 'ssg08'; categoryFilter = 'sniper_rifle'; handleSearch()">SSG 08</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'scar20' }" @click="subCategory = 'scar20'; categoryFilter = 'sniper_rifle'; handleSearch()">SCAR-20</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'g3sg1' }" @click="subCategory = 'g3sg1'; categoryFilter = 'sniper_rifle'; handleSearch()">G3SG1</div>
              </div>
            </div>

            <div class="category-item-wrapper">
              <div class="category-item" :class="{ active: categoryFilter === 'pistol' }">
                <div class="category-icon">
                  <el-icon><Aim /></el-icon>
                </div>
                <span class="category-name">手枪</span>
                <el-icon class="arrow-icon"><ArrowDown /></el-icon>
              </div>
              <div class="category-dropdown">
                <div class="dropdown-item" :class="{ active: subCategory === '' }" @click="subCategory = ''; categoryFilter = 'pistol'; handleSearch()">全部手枪</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'deagle' }" @click="subCategory = 'deagle'; categoryFilter = 'pistol'; handleSearch()">沙漠之鹰</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'glock' }" @click="subCategory = 'glock'; categoryFilter = 'pistol'; handleSearch()">格洛克 18 型</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'p250' }" @click="subCategory = 'p250'; categoryFilter = 'pistol'; handleSearch()">P250</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'usp' }" @click="subCategory = 'usp'; categoryFilter = 'pistol'; handleSearch()">USP 消音版</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'p2000' }" @click="subCategory = 'p2000'; categoryFilter = 'pistol'; handleSearch()">P2000</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'fn57' }" @click="subCategory = 'fn57'; categoryFilter = 'pistol'; handleSearch()">FN57</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'r8' }" @click="subCategory = 'r8'; categoryFilter = 'pistol'; handleSearch()">R8 左轮手枪</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'tec9' }" @click="subCategory = 'tec9'; categoryFilter = 'pistol'; handleSearch()">Tec-9</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'elite' }" @click="subCategory = 'elite'; categoryFilter = 'pistol'; handleSearch()">双持贝瑞塔</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'cz75a' }" @click="subCategory = 'cz75a'; categoryFilter = 'pistol'; handleSearch()">CZ75 自动手枪</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'revolver' }" @click="subCategory = 'revolver'; categoryFilter = 'pistol'; handleSearch()">电击枪</div>
              </div>
            </div>
            
            <div class="category-item-wrapper">
              <div class="category-item" :class="{ active: categoryFilter === 'smg' }">
                <div class="category-icon">
                  <el-icon><Aim /></el-icon>
                </div>
                <span class="category-name">微型冲锋枪</span>
                <el-icon class="arrow-icon"><ArrowDown /></el-icon>
              </div>
              <div class="category-dropdown">
                <div class="dropdown-item" :class="{ active: subCategory === '' }" @click="subCategory = ''; categoryFilter = 'smg'; handleSearch()">全部冲锋枪</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'mp9' }" @click="subCategory = 'mp9'; categoryFilter = 'smg'; handleSearch()">MP9</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'mac10' }" @click="subCategory = 'mac10'; categoryFilter = 'smg'; handleSearch()">MAC-10</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'ump45' }" @click="subCategory = 'ump45'; categoryFilter = 'smg'; handleSearch()">UMP-45</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'p90' }" @click="subCategory = 'p90'; categoryFilter = 'smg'; handleSearch()">P90</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'mp7' }" @click="subCategory = 'mp7'; categoryFilter = 'smg'; handleSearch()">MP7</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'bizon' }" @click="subCategory = 'bizon'; categoryFilter = 'smg'; handleSearch()">PP-野牛</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'mp5sd' }" @click="subCategory = 'mp5sd'; categoryFilter = 'smg'; handleSearch()">MP5-SD</div>
              </div>
            </div>
            
            <div class="category-item-wrapper">
              <div class="category-item" :class="{ active: categoryFilter === 'shotgun' }">
                <div class="category-icon">
                  <el-icon><Aim /></el-icon>
                </div>
                <span class="category-name">霰弹枪</span>
                <el-icon class="arrow-icon"><ArrowDown /></el-icon>
              </div>
              <div class="category-dropdown">
                <div class="dropdown-item" :class="{ active: subCategory === '' }" @click="subCategory = ''; categoryFilter = 'shotgun'; handleSearch()">全部霰弹枪</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'mag7' }" @click="subCategory = 'mag7'; categoryFilter = 'shotgun'; handleSearch()">MAG-7</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'nova' }" @click="subCategory = 'nova'; categoryFilter = 'shotgun'; handleSearch()">新星</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'xm1014' }" @click="subCategory = 'xm1014'; categoryFilter = 'shotgun'; handleSearch()">XM1014</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'sawedoff' }" @click="subCategory = 'sawedoff'; categoryFilter = 'shotgun'; handleSearch()">截短霰弹枪</div>
              </div>
            </div>
            
            <div class="category-item-wrapper">
              <div class="category-item" :class="{ active: categoryFilter === 'machinegun' }">
                <div class="category-icon">
                  <el-icon><Aim /></el-icon>
                </div>
                <span class="category-name">机枪</span>
                <el-icon class="arrow-icon"><ArrowDown /></el-icon>
              </div>
              <div class="category-dropdown">
                <div class="dropdown-item" :class="{ active: subCategory === '' }" @click="subCategory = ''; categoryFilter = 'machinegun'; handleSearch()">全部机枪</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'm249' }" @click="subCategory = 'm249'; categoryFilter = 'machinegun'; handleSearch()">M249</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'negev' }" @click="subCategory = 'negev'; categoryFilter = 'machinegun'; handleSearch()">内格夫</div>
              </div>
            </div>
            
            <div class="category-item-wrapper">
              <div class="category-item" :class="{ active: categoryFilter === 'sticker' }">
                <div class="category-icon">
                  <el-icon><Tickets /></el-icon>
                </div>
                <span class="category-name">印花</span>
                <el-icon class="arrow-icon"><ArrowDown /></el-icon>
              </div>
              <div class="category-dropdown">
                <div class="dropdown-item" :class="{ active: subCategory === '' }" @click="subCategory = ''; categoryFilter = 'sticker'; handleSearch()">全部印花</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'major' }" @click="subCategory = 'major'; categoryFilter = 'sticker'; handleSearch()">Major 赛事</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'community' }" @click="subCategory = 'community'; categoryFilter = 'sticker'; handleSearch()">社区印花</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'other' }" @click="subCategory = 'other'; categoryFilter = 'sticker'; handleSearch()">其他印花</div>
              </div>
            </div>
            
            <div class="category-item-wrapper">
              <div class="category-item" :class="{ active: categoryFilter === 'charm' }">
                <div class="category-icon">
                  <el-icon><Connection /></el-icon>
                </div>
                <span class="category-name">挂件</span>
                <el-icon class="arrow-icon"><ArrowDown /></el-icon>
              </div>
              <div class="category-dropdown">
                <div class="dropdown-item" :class="{ active: subCategory === '' }" @click="subCategory = ''; categoryFilter = 'charm'; handleSearch()">全部挂件</div>
                <div class="dropdown-item" :class="{ active: subCategory === '2024' }" @click="subCategory = '2024'; categoryFilter = 'charm'; handleSearch()">2024 系列</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'other' }" @click="subCategory = 'other'; categoryFilter = 'charm'; handleSearch()">其他挂件</div>
              </div>
            </div>
            
            <div class="category-item-wrapper">
              <div class="category-item" :class="{ active: categoryFilter === 'agent' }">
                <div class="category-icon">
                  <el-icon><Box /></el-icon>
                </div>
                <span class="category-name">探员</span>
                <el-icon class="arrow-icon"><ArrowDown /></el-icon>
              </div>
              <div class="category-dropdown">
                <div class="dropdown-item" :class="{ active: subCategory === '' }" @click="subCategory = ''; categoryFilter = 'agent'; handleSearch()">全部探员</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'ct' }" @click="subCategory = 'ct'; categoryFilter = 'agent'; handleSearch()">反恐精英</div>
                <div class="dropdown-item" :class="{ active: subCategory === 't' }" @click="subCategory = 't'; categoryFilter = 'agent'; handleSearch()">恐怖分子</div>
              </div>
            </div>
            
            <div class="category-item-wrapper">
              <div class="category-item" :class="{ active: categoryFilter === 'other' }">
                <div class="category-icon">
                  <el-icon><Folder /></el-icon>
                </div>
                <span class="category-name">其他</span>
                <el-icon class="arrow-icon"><ArrowDown /></el-icon>
              </div>
              <div class="category-dropdown">
                <div class="dropdown-item" :class="{ active: subCategory === '' }" @click="subCategory = ''; categoryFilter = 'other'; handleSearch()">全部其他</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'case' }" @click="subCategory = 'case'; categoryFilter = 'other'; handleSearch()">武器箱</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'music' }" @click="subCategory = 'music'; categoryFilter = 'other'; handleSearch()">音乐盒</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'tool' }" @click="subCategory = 'tool'; categoryFilter = 'other'; handleSearch()">工具</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'graffiti' }" @click="subCategory = 'graffiti'; categoryFilter = 'other'; handleSearch()">涂鸦</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'pass' }" @click="subCategory = 'pass'; categoryFilter = 'other'; handleSearch()">通行证</div>
                <div class="dropdown-item" :class="{ active: subCategory === 'collectible' }" @click="subCategory = 'collectible'; categoryFilter = 'other'; handleSearch()">收藏品</div>
              </div>
            </div>
          </div>
          
          <!-- 筛选栏 -->
          <div class="filter-row">
            <div class="filter-group">
              <el-select v-model="filters.quality" placeholder="品质" clearable class="filter-select" @change="handleSearch">
                <el-option label="不限品质" value="" />
                <el-option label="违禁" value="Contraband" />
                <el-option label="隐秘" value="Covert" />
                <el-option label="保密" value="Classified" />
                <el-option label="受限" value="Restricted" />
                <el-option label="军规级" value="MilSpec" />
                <el-option label="工业级" value="Industrial" />
                <el-option label="消费级" value="Consumer" />
              </el-select>

              <el-select v-model="filters.type" placeholder="类别" clearable class="filter-select" @change="handleSearch">
                <el-option label="不限类别" value="" />
                <el-option label="普通" value="Normal" />
                <el-option label="纪念品" value="Souvenir" />
                <el-option label="StatTrak™" value="StatTrak" />
                <el-option label="★" value="Star" />
              </el-select>

              <el-select v-model="filters.exterior" placeholder="外观" clearable class="filter-select" @change="handleSearch">
                <el-option label="不限外观" value="" />
                <el-option label="崭新出厂" value="FN" />
                <el-option label="略有磨损" value="MW" />
                <el-option label="久经沙场" value="FT" />
                <el-option label="破损不堪" value="WW" />
                <el-option label="战痕累累" value="BS" />
              </el-select>
            </div>

            <div class="search-group">
              <el-input
                v-model="searchKeyword"
                placeholder="输入物品名称"
                clearable
                class="search-input"
                @keyup.enter="handleSearch"
                @clear="handleSearch"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-button type="primary" class="search-btn" @click="handleSearch">
                <el-icon><Search /></el-icon>
                搜索
              </el-button>
            </div>
          </div>
        </div>
      </section>

      <!-- 市场列表区域 -->
      <section class="market-section">
        <div class="market-tabs">
          <div class="tab-item active">出售</div>
          <div class="tab-item">求购</div>
          <div class="tab-right">
            <span class="price-label">¥</span>
            <el-input
              v-model="minPrice"
              placeholder="最低价"
              class="price-input"
              @keyup.enter="handleSearch"
            />
            <span class="price-separator">-</span>
            <el-input
              v-model="maxPrice"
              placeholder="最高价"
              class="price-input"
              @keyup.enter="handleSearch"
            />
            <el-select v-model="sortValue" placeholder="排序" class="sort-select" @change="handleSearch">
              <el-option label="排序" value="default" />
              <el-option label="价格从低到高" value="price_asc" />
              <el-option label="价格从高到低" value="price_desc" />
              <el-option label="最新发布" value="newest" />
            </el-select>
          </div>
        </div>

        <div class="items-grid" v-loading="loading">
          <el-empty v-if="!pagedItems.length && !loading" description="暂无符合条件的在售记录" />

          <div v-for="entry in pagedItems" v-else :key="entry.id" class="item-card" @click="goToDetail(entry)">
            <div class="card-image">
              <div class="wear-tag">{{ getExteriorText(getItemExterior(entry)) }}</div>
              <img :src="getItemIcon(entry)" :alt="getItemName(entry)" />
            </div>
            <div class="card-info">
              <h4 class="card-name">{{ getItemName(entry) }}</h4>
              <p class="card-meta">{{ getCategoryDisplayText(entry) }}</p>
              <div class="card-footer">
                <p class="card-price">¥ {{ formatPrice(entry.price) }}</p>
                <span class="seller-text">{{ entry.user?.username || '查看详情' }}</span>
              </div>
            </div>
          </div>
        </div>

        <div v-if="total > pageSize" class="pagination-wrap">
          <el-pagination
            background
            layout="prev, pager, next"
            :current-page="page"
            :page-size="pageSize"
            :total="total"
            @current-change="handlePageChange"
          />
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Grid, Aim, Tickets, Connection, Box, Folder, ArrowDown } from '@element-plus/icons-vue'
import { getMarketList } from '@/api/sellOrder'
import LoginModal from '@/components/LoginModal.vue'
import SiteHeader from '@/components/SiteHeader.vue'
import { normalizeQualityKey, resolveItemCategory, resolveItemSubCategory, resolveItemType } from '@/utils/itemClassification'
import {
  normalizeWearValue,
  resolveExterior as resolveExteriorCode
} from '@/utils/itemExterior'

const router = useRouter()
const showLoginModal = ref(false)
const loading = ref(false)
const items = ref([])
const page = ref(1)
const pageSize = ref(20)
const searchKeyword = ref('')
const minPrice = ref('')
const maxPrice = ref('')
const sortValue = ref('default')

const MARKET_FETCH_PAGE_SIZE = 200
const MARKET_FETCH_MAX_PAGES = 20

const categoryFilter = ref('')
const subCategory = ref('')

const filters = reactive({
  quality: '',
  type: '',
  exterior: ''
})

const categoryTextMap = {
  rifle: '\u6b65\u67aa',
  pistol: '\u624b\u67aa',
  smg: '\u51b2\u950b\u67aa',
  shotgun: '\u9730\u5f39\u67aa',
  sniper_rifle: '\u72d9\u51fb\u6b65\u67aa',
  machinegun: '\u673a\u67aa',
  knife: '\u5200\u5177',
  glove: '\u624b\u5957',
  sticker: '\u5370\u82b1',
  charm: '\u6302\u4ef6',
  agent: '\u63a2\u5458',
  other: '\u5176\u4ed6'
}
const exteriorTextMap = {
  FN: '\u5d2d\u65b0\u51fa\u5382',
  MW: '\u7565\u6709\u78e8\u635f',
  FT: '\u4e45\u7ecf\u6c99\u573a',
  WW: '\u7834\u635f\u4e0d\u582a',
  BS: '\u6218\u75d5\u7d2f\u7d2f',
  NoPaint: '\u65e0\u6d82\u88c5'
}
const resolvedCategoryTextMap = {
  rifle: '\u6b65\u67aa',
  pistol: '\u624b\u67aa',
  smg: '\u51b2\u950b\u67aa',
  shotgun: '\u9730\u5f39\u67aa',
  sniper_rifle: '\u72d9\u51fb\u6b65\u67aa',
  machinegun: '\u673a\u67aa',
  knife: '\u5200\u5177',
  glove: '\u624b\u5957',
  sticker: '\u5370\u82b1',
  charm: '\u6302\u4ef6',
  agent: '\u63a2\u5458',
  other: '\u5176\u4ed6'
}
const resolvedSubCategoryTextMap = {
  graffiti: '\u6d82\u9e26',
  case: '\u6b66\u5668\u7bb1',
  music: '\u97f3\u4e50\u76d2',
  tool: '\u5de5\u5177',
  pass: '\u901a\u884c\u8bc1',
  collectible: '\u6536\u85cf\u54c1'
}
const resolvedExteriorTextMap = {
  FN: '\u5d2d\u65b0\u51fa\u5382',
  MW: '\u7565\u6709\u78e8\u635f',
  FT: '\u4e45\u7ecf\u6c99\u573a',
  WW: '\u7834\u635f\u4e0d\u582a',
  BS: '\u6218\u75d5\u7d2f\u7d2f',
  NoPaint: '\u65e0\u6d82\u88c5'
}
const normalizeRarity = (value) => {
  const normalized = String(value || '').trim().toLowerCase()
  const map = {
    ancient: 'contraband',
    contraband: 'contraband',
    immortal: 'extraordinary',
    extraordinary: 'extraordinary',
    legendary: 'covert',
    covert: 'covert',
    mythical: 'classified',
    classified: 'classified',
    rare: 'restricted',
    restricted: 'restricted',
    uncommon: 'mil-spec',
    'mil-spec': 'mil-spec',
    industrial: 'industrial',
    common: 'consumer',
    consumer: 'consumer',
    remarkable: 'remarkable'
  }
  return map[normalized] || normalized
}

const normalizeExterior = (value, ...fallbackTexts) => {
  const normalized = String(value || '').trim()
  const map = {
    factory_new: 'FN',
    'factory new': 'FN',
    崭新出厂: 'FN',
    minimal_wear: 'MW',
    'minimal wear': 'MW',
    略有磨损: 'MW',
    'field-tested': 'FT',
    'field tested': 'FT',
    久经沙场: 'FT',
    'well-worn': 'WW',
    'well worn': 'WW',
    破损不堪: 'WW',
    'battle-scarred': 'BS',
    'battle scarred': 'BS',
    战痕累累: 'BS'
  }

  if (map[normalized.toLowerCase()] || map[normalized]) {
    return map[normalized.toLowerCase()] || map[normalized]
  }

  for (const raw of fallbackTexts) {
    const text = String(raw || '').toLowerCase()
    if (text.includes('factory new')) return 'FN'
    if (text.includes('minimal wear')) return 'MW'
    if (text.includes('field-tested') || text.includes('field tested')) return 'FT'
    if (text.includes('well-worn') || text.includes('well worn')) return 'WW'
    if (text.includes('battle-scarred') || text.includes('battle scarred')) return 'BS'
  }

  return normalized
}

const fetchItems = async () => {
  loading.value = true
  try {
    const merged = []
    let currentPage = 1
    let expectedTotal = Number.POSITIVE_INFINITY

    while (currentPage <= MARKET_FETCH_MAX_PAGES && merged.length < expectedTotal) {
      const res = await getMarketList({
        page: currentPage,
        size: MARKET_FETCH_PAGE_SIZE,
        sortField: 'created_at',
        sortOrder: 'DESC'
      })

      const list = Array.isArray(res?.list) ? res.list : []
      expectedTotal = Number(res?.total || list.length || 0)
      merged.push(...list)

      if (!list.length || list.length < MARKET_FETCH_PAGE_SIZE) {
        break
      }

      currentPage += 1
    }

    items.value = merged
  } catch (error) {
    ElMessage.error(error?.message || '获取市场列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
}

const resetFilters = () => {
  searchKeyword.value = ''
  minPrice.value = ''
  maxPrice.value = ''
  sortValue.value = 'default'
  categoryFilter.value = ''
  subCategory.value = ''
  filters.quality = ''
  filters.type = ''
  filters.exterior = ''
  page.value = 1
}

const handlePageChange = (current) => {
  page.value = current
}

const goToDetail = (entry) => {
  router.push(`/items/order/${entry.id}`)
}

const getItemName = (entry) => entry.inventory?.name || entry.item?.nameCn || entry.item?.name || '\u672a\u77e5\u9970\u54c1'
const getItemIcon = (entry) => entry.inventory?.iconUrl || entry.item?.iconUrl || '/default-item.png'
const getItemExterior = (entry) =>
  resolveExteriorCode(
    normalizeWearValue(entry?.inventory?.paintWear ?? entry?.paintWear),
    entry.inventory?.exterior || entry.inventory?.wearName || entry.item?.exterior,
    entry.inventory?.name,
    entry.item?.nameCn,
    entry.item?.name
  )
const getCategoryText = (value) => resolvedCategoryTextMap[value] || categoryTextMap[value] || value || '\u5176\u4ed6'
const getExteriorText = (value) => resolvedExteriorTextMap[value] || exteriorTextMap[value] || value || '\u672a\u77e5\u5916\u89c2'
const formatPrice = (price) => Number(price || 0).toFixed(2)
const getResolvedCategory = (entry) => resolveItemCategory(entry)
const getResolvedSubCategory = (entry) => resolveItemSubCategory(entry)
const getResolvedQuality = (entry) => normalizeQualityKey(normalizeRarity(entry.inventory?.rarity || entry.item?.quality || entry.item?.rarity))
const getResolvedTypeValue = (entry) => resolveItemType(entry)
const getCategoryDisplayText = (entry) => {
  const category = getResolvedCategory(entry)
  const subCategoryText = resolvedSubCategoryTextMap[getResolvedSubCategory(entry)]
  return category === 'other' && subCategoryText ? subCategoryText : getCategoryText(category)
}
const filteredItems = computed(() => {
  let list = [...items.value]

  const keyword = String(searchKeyword.value || '').trim().toLowerCase()
  if (keyword) {
    list = list.filter((entry) => {
      const searchable = [
        getItemName(entry),
        entry.item?.nameCn,
        entry.item?.name,
        entry.inventory?.name,
        entry.user?.username
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
      return searchable.includes(keyword)
    })
  }

  if (categoryFilter.value) {
    list = list.filter((entry) => getResolvedCategory(entry) === categoryFilter.value)
  }

  if (subCategory.value) {
    list = list.filter((entry) => getResolvedSubCategory(entry) === subCategory.value)
  }

  if (filters.quality) {
    const targetQuality = normalizeQualityKey(filters.quality)
    list = list.filter((entry) => getResolvedQuality(entry) === targetQuality)
  }

  if (filters.type) {
    list = list.filter((entry) => getResolvedTypeValue(entry) === filters.type)
  }

  if (filters.exterior) {
    list = list.filter((entry) => getItemExterior(entry) === filters.exterior)
  }

  const min = Number(minPrice.value)
  if (Number.isFinite(min) && minPrice.value !== '') {
    list = list.filter((entry) => Number(entry.price || 0) >= min)
  }

  const max = Number(maxPrice.value)
  if (Number.isFinite(max) && maxPrice.value !== '') {
    list = list.filter((entry) => Number(entry.price || 0) <= max)
  }

  list.sort((left, right) => {
    if (sortValue.value === 'price_asc') {
      return Number(left.price || 0) - Number(right.price || 0)
    }
    if (sortValue.value === 'price_desc') {
      return Number(right.price || 0) - Number(left.price || 0)
    }

    const leftTime = new Date(left.createdAt || 0).getTime()
    const rightTime = new Date(right.createdAt || 0).getTime()
    return rightTime - leftTime
  })

  return list
})

const total = computed(() => filteredItems.value.length)
const pagedItems = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredItems.value.slice(start, start + pageSize.value)
})

onMounted(() => {
  fetchItems()
})
</script>

<style scoped>
.market-page {
  min-height: 100vh;
  background: #e3e3e3;
}

.main-content {
  padding-top: 100px;
}

.category-section {
  background: linear-gradient(135deg, rgba(18, 22, 31, 0.92), rgba(31, 48, 67, 0.88));
  padding: 20px 0 16px;
  margin-bottom: 20px;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(10px);
  position: relative;
  z-index: 10;
}

.category-container {
  width: min(1240px, calc(100% - 40px));
  margin: 0 auto;
}

.category-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px 18px;
  background: rgba(42, 47, 58, 0.6);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 75px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.category-item:hover {
  background: rgba(53, 59, 71, 0.7);
  transform: translateY(-2px);
  border-color: rgba(255, 255, 255, 0.15);
}

.category-item.active {
  background: rgba(240, 179, 33, 0.9);
  color: #fff;
  border-color: rgba(240, 179, 33, 0.9);
}

.category-icon {
  font-size: 24px;
  color: #f0b321;
}

.category-item.active .category-icon {
  color: #fff;
}

.category-name {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
  white-space: nowrap;
}

.category-item.active .category-name {
  color: #fff;
}

.category-item-wrapper {
  position: relative;
  z-index: 100;
  padding-bottom: 8px;
  margin-bottom: -8px;
}

.category-item-wrapper .category-item {
  position: relative;
}

.arrow-icon {
  position: absolute;
  top: 4px;
  right: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

.category-item-wrapper:hover .category-dropdown {
  display: grid;
}

.category-dropdown {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(30, 35, 45, 0.98);
  border-radius: 8px;
  padding: 16px 12px 12px;
  min-width: 280px;
  max-height: 400px;
  overflow-y: auto;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.1);
  z-index: 10000;
  backdrop-filter: blur(10px);
  display: none;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px;
}

.dropdown-item {
  padding: 8px 12px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  border-radius: 6px;
}

.dropdown-item:hover {
  background: rgba(53, 59, 71, 0.6);
  color: #f0b321;
}

.dropdown-item.active {
  background: #f0b321;
  color: #fff;
}

.filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 0 8px;
}

.filter-group {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-select {
  width: 110px;
}

.filter-select :deep(.el-input__wrapper),
.filter-select :deep(.el-select__wrapper) {
  background: rgba(42, 47, 58, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: none;
}

.filter-select :deep(.el-input__inner),
.filter-select :deep(.el-select__input) {
  color: rgba(255, 255, 255, 0.9);
}

.filter-select :deep(.el-select__selected-item) {
  color: rgba(255, 255, 255, 0.9);
}

.filter-select :deep(.el-select__prefix) {
  color: rgba(255, 255, 255, 0.5);
}

.search-group {
  display: flex;
  gap: 8px;
  align-items: center;
}

.search-input {
  width: 240px;
}

.search-input :deep(.el-input__wrapper) {
  background: rgba(42, 47, 58, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: none;
}

.search-input :deep(.el-input__inner) {
  color: rgba(255, 255, 255, 0.9);
}

.search-input :deep(.el-input__prefix) {
  color: rgba(255, 255, 255, 0.5);
}

.search-input :deep(.el-input__placeholder) {
  color: rgba(255, 255, 255, 0.4);
}

.search-btn {
  background: #f0b321;
  border-color: #f0b321;
  color: #fff;
}

.search-btn:hover {
  background: #ffb929;
  border-color: #ffb929;
}

.market-section {
  width: min(1240px, calc(100% - 40px));
  margin: 0 auto 36px;
  background: #fff;
  border: 1px solid #e6e8ec;
  border-radius: 18px;
  box-shadow: 0 20px 48px rgba(17, 24, 39, 0.08);
  overflow: visible;
  position: relative;
  z-index: 1;
}

.market-tabs {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #f7f7f7;
  border-bottom: 1px solid #e6e8ec;
}

.tab-item {
  display: flex;
  align-items: center;
  min-height: 52px;
  padding: 0 20px;
  color: #666f7d;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  border-bottom: 2px solid transparent;
}

.tab-item:hover {
  color: #f0b321;
}

.tab-item.active {
  color: #f0b321;
  border-bottom-color: #f0b321;
}

.tab-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.price-label {
  color: #666;
  font-size: 14px;
  font-weight: 600;
}

.price-input {
  width: 100px;
}

.price-input :deep(.el-input__wrapper) {
  background: #fff;
  box-shadow: inset 0 0 0 1px #d9dde5;
}

.price-separator {
  color: #999;
  font-size: 14px;
}

.sort-select {
  width: 120px;
}

.sort-select :deep(.el-input__wrapper) {
  background: #fff;
  box-shadow: inset 0 0 0 1px #d9dde5;
}

.items-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 22px;
  padding: 24px 24px 28px;
}

.item-card {
  overflow: hidden;
  border: 1px solid #ececec;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.item-card:hover {
  transform: translateY(-2px);
  border-color: #d9dde5;
  box-shadow: 0 18px 30px rgba(17, 24, 39, 0.12);
}

.card-image {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 180px;
  padding: 18px;
  background: linear-gradient(180deg, #f8f8f8 0%, #efefef 100%);
}

.card-image img {
  max-width: 100%;
  max-height: 132px;
  object-fit: contain;
}

.wear-tag {
  position: absolute;
  left: 8px;
  top: 8px;
  padding: 5px 8px;
  border-radius: 999px;
  background: #5d8a42;
  color: #fff;
  font-size: 12px;
}

.item-icons {
  position: absolute;
  right: 8px;
  top: 8px;
}

.icon-badge {
  padding: 4px 8px;
  border-radius: 999px;
  background: #8e8e8e;
  color: #fff;
  font-size: 12px;
}

.card-info {
  padding: 14px;
}

.card-name {
  display: -webkit-box;
  min-height: 50px;
  margin: 0 0 8px;
  overflow: hidden;
  color: #1f2937;
  font-size: 15px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.card-meta {
  margin: 0 0 8px;
  color: #6b7280;
  font-size: 13px;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.card-price {
  margin: 0;
  color: #f0b321;
  font-size: 18px;
  font-weight: 700;
}

.seller-text {
  color: #9ca3af;
  font-size: 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 0 0 28px;
}

@media (max-width: 1024px) {
  .items-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-group {
    justify-content: center;
  }

  .search-group {
    justify-content: center;
  }

  .market-tabs {
    flex-direction: column;
    gap: 12px;
    padding: 16px;
  }

  .tab-right {
    width: 100%;
    justify-content: center;
  }

  .items-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .category-row {
    justify-content: center;
  }

  .items-grid {
    grid-template-columns: 1fr;
  }
}
</style>

