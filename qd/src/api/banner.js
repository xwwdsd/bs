import { get, post, put, del } from '@/utils/request'

export function getActiveBanners() {
  return get('/v1/banners')
}

export function getAllBanners() {
  return get('/v1/admin/banners')
}

export function createBanner(data) {
  return post('/v1/admin/banners', data)
}

export function updateBanner(data) {
  return put(`/v1/admin/banners/${data.id}`, data)
}

export function deleteBanner(id) {
  return del(`/v1/admin/banners/${id}`)
}

export function updateBannerStatus(id, status) {
  return put(`/v1/admin/banners/${id}/status`, null, { params: { status } })
}
