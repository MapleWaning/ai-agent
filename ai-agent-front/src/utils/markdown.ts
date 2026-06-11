import MarkdownIt from 'markdown-it'

// html:false 禁止渲染原始 HTML，降低 XSS 风险（未引入额外的 DOMPurify 以保持依赖精简）
const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
})

/** 将 Markdown 文本渲染为 HTML 字符串 */
export function renderMarkdown(text: string): string {
  if (!text) return ''
  return md.render(text)
}
