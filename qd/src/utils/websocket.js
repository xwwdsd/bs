import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useUserStore } from '@/stores/user'

class WebSocketService {
  constructor() {
    this.client = null
    this.subscriptions = new Map()
    this.connected = false
  }

  connect() {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) {
      return
    }

    if (this.client?.active || this.connected) {
      return
    }

    const token = userStore.accessToken
    const userId = userStore.userInfo?.userId
    if (!token || !userId) {
      return
    }

    const wsUrl = import.meta.env.VITE_WS_URL || window.location.origin

    this.client = new Client({
      webSocketFactory: () => new SockJS(`${wsUrl}/ws`),
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      debug: () => {},
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000
    })

    this.client.onConnect = () => {
      this.connected = true
      this.subscribeToOrders(userId)
    }

    this.client.onDisconnect = () => {
      this.connected = false
    }

    this.client.onStompError = () => {
      this.connected = false
    }

    this.client.activate()
  }

  subscribeToOrders(userId) {
    if (!userId || !this.connected || !this.client) {
      return
    }

    const destination = `/topic/user/${userId}/orders`
    const previous = this.subscriptions.get('orders')
    previous?.unsubscribe()

    const subscription = this.client.subscribe(destination, (message) => {
      try {
        const payload = JSON.parse(message.body)
        this.handleOrderMessage(payload)
      } catch (error) {
        // Ignore malformed messages to keep the app stable.
      }
    })

    this.subscriptions.set('orders', subscription)
  }

  handleOrderMessage(payload) {
    window.dispatchEvent(new CustomEvent('ws-order-update', { detail: payload }))
    this.showNotification('订单通知', payload?.message || '订单状态已更新')
  }

  showNotification(title, body) {
    if (!('Notification' in window)) {
      return
    }

    const icon = '/favicon.ico'

    if (Notification.permission === 'granted') {
      new Notification(title, { body, icon })
      return
    }

    if (Notification.permission !== 'denied') {
      Notification.requestPermission().then((permission) => {
        if (permission === 'granted') {
          new Notification(title, { body, icon })
        }
      })
    }
  }

  disconnect() {
    if (!this.client) {
      return
    }

    this.subscriptions.forEach((subscription) => subscription.unsubscribe())
    this.subscriptions.clear()
    this.client.deactivate()
    this.client = null
    this.connected = false
  }

  isConnected() {
    return this.connected
  }
}

const websocketService = new WebSocketService()

export default websocketService
