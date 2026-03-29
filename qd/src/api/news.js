import { get, post, put, del } from '@/utils/request'

export function getNews(category) {
  return get('/v1/news', { category })
}

export function getNewsById(id) {
  return get(`/v1/news/${id}`)
}

export function getMyNews() {
  return get('/v1/news/my')
}

export function createNews(data) {
  return post('/v1/news', data)
}

export function updateNews(id, data) {
  return put(`/v1/news/${id}`, data)
}

export function deleteNews(id) {
  return del(`/v1/news/${id}`)
}
