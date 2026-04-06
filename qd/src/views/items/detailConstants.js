export const EXTERIOR_ORDER = ['FN', 'MW', 'FT', 'WW', 'BS']
export const TYPE_SWITCH_CODE = 'StatTrak'

export const TEXT = {
  market: '\u9970\u54c1\u5e02\u573a',
  quality: '\u54c1\u8d28',
  category: '\u7c7b\u522b',
  type: '\u7c7b\u578b',
  notice: '\u516c\u544a',
  referencePrice: '\u53c2\u8003\u4ef7\u683c',
  currentPrice: '\u5f53\u524d\u552e\u4ef7',
  buyCurrentOrder: '\u8d2d\u4e70\u5f53\u524d\u8ba2\u5355',
  sellMine: '\u6211\u8981\u51fa\u552e',
  favorite: '\u6536\u85cf',
  favorited: '\u5df2\u6536\u85cf',
  seller: '\u5356\u5bb6',
  wear: '\u78e8\u635f',
  listedAt: '\u4e0a\u67b6\u65f6\u95f4',
  currentOnSale: '\u5f53\u524d\u5728\u552e',
  saleRecordSuffix: '\u6761\u5728\u552e\u8bb0\u5f55',
  emptyListings: '\u6682\u65e0\u540c\u6b3e\u5728\u552e\u8bb0\u5f55',
  item: '\u9970\u54c1',
  wearLevel: '\u78e8\u635f\u5ea6',
  price: '\u4ef7\u683c',
  action: '\u64cd\u4f5c',
  bargain: '\u8fd8\u4ef7',
  bargainTitle: '\u53d1\u8d77\u8fd8\u4ef7',
  currentSalePrice: '\u5f53\u524d\u552e\u4ef7',
  bargainHint: '\u5356\u5bb6\u63a5\u53d7\u540e\uff0c\u7cfb\u7edf\u4f1a\u6309\u8fd8\u4ef7\u91d1\u989d\u81ea\u52a8\u521b\u5efa\u8ba2\u5355\u3002',
  bargainPriceLabel: '\u8fd8\u4ef7\u91d1\u989d',
  sendBargain: '\u53d1\u9001\u8fd8\u4ef7',
  currentViewing: '\u5f53\u524d\u67e5\u770b',
  viewDetail: '\u67e5\u770b\u8be6\u60c5',
  buy: '\u8d2d\u4e70',
  notFound: '\u672a\u627e\u5230\u5bf9\u5e94\u7684\u9970\u54c1\u6216\u8ba2\u5355',
  createSellOrder: '\u521b\u5efa\u51fa\u552e\u8ba2\u5355',
  chooseInventory: '\u9009\u62e9\u5e93\u5b58\u9970\u54c1',
  chooseInventoryPlaceholder: '\u8bf7\u9009\u62e9\u5e93\u5b58\u9970\u54c1',
  sellPrice: '\u51fa\u552e\u4ef7\u683c',
  noComparableInventory: '\u4f60\u5f53\u524d\u6ca1\u6709\u53ef\u51fa\u552e\u7684\u540c\u6b3e\u5e93\u5b58',
  cancel: '\u53d6\u6d88',
  confirmSell: '\u786e\u8ba4\u51fa\u552e',
  currency: '\u00a5',
  unknownSeller: '\u672a\u77e5\u5356\u5bb6',
  unknownItem: '\u672a\u77e5\u9970\u54c1',
  noActiveSale: '\u6682\u65e0\u5728\u552e',
  confirmBuyTitle: '\u786e\u8ba4\u8d2d\u4e70',
  confirmLabel: '\u786e\u8ba4',
  genericTradeNotice:
    '\u4e3a\u7ef4\u62a4\u4ea4\u6613\u516c\u5e73\uff0c\u4efb\u610f\u4e00\u65b9\u5728\u4ea4\u6613\u5b8c\u6210\u524d\u64a4\u9500\u4ea4\u6613\uff0c\u8d26\u53f7\u5c06\u53d7\u5230\u9650\u5236\u6216\u5904\u7f5a\u3002\u8bf7\u5728\u4ea4\u6613\u5b8c\u6210\u524d\u907f\u514d\u53d6\u6d88\uff0c\u4ee5\u514d\u9020\u6210\u635f\u5931\u3002',
  saleStatus: '\u5728\u552e\u4e2d',
  restrictedStatus: '\u4ea4\u6613\u53d7\u9650',
  unknownExterior: '\u672a\u77e5\u5916\u89c2',
  unknownQuality: '\u672a\u77e5',
  normalType: '\u666e\u901a',
  unknownUserLevel: '\u666e\u901a\u7528\u6237',
  fetchDetailFailed: '\u83b7\u53d6\u8be6\u60c5\u5931\u8d25',
  confirmBuyMessagePrefix: '\u786e\u8ba4\u4ee5',
  confirmBuyMessageSuffix: '\u8d2d\u4e70\u8fd9\u4ef6\u9970\u54c1\u5417\uff1f',
  orderCreatedSuccess: '\u8ba2\u5355\u521b\u5efa\u6210\u529f\uff0c\u8bf7\u524d\u5f80\u201c\u6211\u7684\u8ba2\u5355\u201d\u7ee7\u7eed\u5904\u7406',
  buyFailed: '\u8d2d\u4e70\u5931\u8d25',
  lowestPriceBargainDisabled: '\u8fd9\u6761\u5356\u5355\u5df2\u7ecf\u662f\u6700\u4f4e\u4ef7\uff0c\u6682\u4e0d\u652f\u6301\u518d\u8fd8\u4ef7',
  targetOrderMissing: '\u672a\u627e\u5230\u76ee\u6807\u5356\u5355',
  inputValidBargainPrice: '\u8bf7\u8f93\u5165\u6709\u6548\u7684\u8fd8\u4ef7\u91d1\u989d',
  bargainPriceTooHigh: '\u8fd8\u4ef7\u91d1\u989d\u9700\u8981\u4f4e\u4e8e\u5f53\u524d\u552e\u4ef7',
  bargainSent: '\u8fd8\u4ef7\u5df2\u53d1\u9001\uff0c\u7b49\u5f85\u5356\u5bb6\u5904\u7406',
  sendBargainFailed: '\u53d1\u9001\u8fd8\u4ef7\u5931\u8d25',
  favoriteSuccess: '\u6536\u85cf\u6210\u529f',
  favoriteRemoved: '\u5df2\u53d6\u6d88\u6536\u85cf',
  favoriteFailed: '\u6536\u85cf\u64cd\u4f5c\u5931\u8d25'
}

export const QUALITY_TEXT_MAP = {
  contraband: '\u8fdd\u7981',
  covert: '\u9690\u79d8',
  classified: '\u4fdd\u5bc6',
  restricted: '\u53d7\u9650',
  'mil-spec': '\u519b\u89c4\u7ea7',
  industrial: '\u5de5\u4e1a\u7ea7',
  extraordinary: '\u975e\u51e1',
  remarkable: '\u5947\u5f02',
  consumer: '\u6d88\u8d39\u7ea7'
}

export const EXTERIOR_TEXT_MAP = {
  FN: '\u5d2d\u65b0\u51fa\u5382',
  MW: '\u7565\u6709\u78e8\u635f',
  FT: '\u4e45\u7ecf\u6c99\u573a',
  WW: '\u7834\u635f\u4e0d\u582a',
  BS: '\u6218\u75d5\u7d2f\u7d2f',
  NoPaint: '\u65e0\u6d82\u88c5'
}

export const WEAR_RANGE_MAP = {
  FN: [0, 0.07],
  MW: [0.07, 0.15],
  FT: [0.15, 0.38],
  WW: [0.38, 0.45],
  BS: [0.45, 1]
}

export const CATEGORY_TEXT_MAP = {
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
  case: '\u6b66\u5668\u7bb1',
  other: '\u5176\u4ed6'
}

export const OTHER_SUBCATEGORY_TEXT_MAP = {
  graffiti: '\u6d82\u9e26',
  case: '\u6b66\u5668\u7bb1',
  music: '\u97f3\u4e50\u76d2',
  tool: '\u5de5\u5177',
  pass: '\u901a\u884c\u8bc1',
  collectible: '\u6536\u85cf\u54c1'
}

export const TYPE_TEXT_MAP = {
  Normal: '\u666e\u901a',
  StatTrak: 'StatTrak\u2122',
  Souvenir: '\u7eaa\u5ff5\u54c1',
  Star: '\u7a00\u6709'
}

export const LEVEL_LABELS = {
  0: '\u7528\u6237',
  1: '\u7528\u6237',
  2: '\u7528\u6237',
  3: '\u7528\u6237'
}
