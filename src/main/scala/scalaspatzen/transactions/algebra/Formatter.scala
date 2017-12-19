package scalaspatzen.transactions.algebra

trait Formatter[ComparisonResults] {
  def toMarkdown(results: ComparisonResults): String
  def markdownToHtml(md: String, css: String): String
}
