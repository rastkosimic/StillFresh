import { useMemo } from 'react'
import { Link } from 'react-router-dom'
import { marked } from 'marked'

type LegalPageProps = {
  title: string
  markdown: string
}

export function LegalPage({ title, markdown }: LegalPageProps) {
  const html = useMemo(() => {
    marked.setOptions({ gfm: true, breaks: false })
    return marked.parse(markdown, { async: false }) as string
  }, [markdown])

  return (
    <div className="page legal-page">
      <Link className="legal-page__back" to="/">
        ← Nazad na početnu
      </Link>
      <article className="legal-doc" aria-label={title}>
        <div dangerouslySetInnerHTML={{ __html: html }} />
      </article>
    </div>
  )
}
