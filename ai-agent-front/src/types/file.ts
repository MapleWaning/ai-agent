/** 文件请求（列表 / 下载 / 删除共用 FileRequest） */
export interface FileRequest {
  chatId: number
  /** 列表接口不需要；下载、删除必填 */
  fileName?: string
}

/** 会话文件（ChatFileVO，列表接口直接返回数组，无 BaseResponse 包装） */
export interface ChatFileVO {
  fileName: string
  size: number
  sizeText: string
  /** 最后修改时间，格式 yyyy-MM-dd HH:mm:ss */
  lastModified: string
  /** 固定为 /api/chat/file/download，需配合 POST 下载 */
  downloadUrl: string
}
