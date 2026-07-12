import { useEffect, useState, type FormEvent } from 'react'
import { Megaphone, Pin, MessageSquare, Send, Trash2, X, PenLine } from 'lucide-react'
import { announcementsApi } from '../api/announcements'
import type { AnnouncementResponse, CommentResponse } from '../types'
import { Card, CardHeader } from '../components/Card'
import { Input, Textarea } from '../components/Form'
import { Button } from '../components/Button'
import { Alert, EmptyState } from '../components/Alert'
import { HttpError } from '../api/client'
import { useAuth } from '../context/AuthContext'

function timeAgo(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime()
  const minutes = Math.floor(diffMs / 60000)
  if (minutes < 1) return 'just now'
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}d ago`
  return new Date(iso).toLocaleDateString()
}

function initialsOf(name: string): string {
  return name
    .split(' ')
    .map((p) => p[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()
}

export function AnnouncementsPage() {
  const { session } = useAuth()
  const canPin = session?.role === 'ROLE_ADMIN' || session?.role === 'ROLE_MANAGER'

  const [posts, setPosts] = useState<AnnouncementResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isComposerOpen, setIsComposerOpen] = useState(false)

  function loadFeed() {
    announcementsApi
      .getFeed()
      .then(setPosts)
      .catch((err) => setError(err instanceof HttpError ? err.message : 'Failed to load announcements'))
      .finally(() => setIsLoading(false))
  }

  useEffect(() => {
    loadFeed()
  }, [])

  async function handleDeletePost(id: number) {
    setError(null)
    try {
      await announcementsApi.remove(id)
      setPosts((prev) => prev.filter((p) => p.id !== id))
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Failed to delete post')
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-ink-900">Announcements</h1>
          <p className="mt-1 text-sm text-ink-500">Company updates, projects, and news — post and discuss.</p>
        </div>
        <Button icon={<PenLine className="h-4 w-4" />} onClick={() => setIsComposerOpen(true)}>
          New post
        </Button>
      </div>

      {error && <Alert variant="error">{error}</Alert>}

      {isComposerOpen && (
        <Composer
          canPin={canPin}
          onClose={() => setIsComposerOpen(false)}
          onCreated={(post) => {
            setIsComposerOpen(false)
            setPosts((prev) => [post, ...prev].sort((a, b) => Number(b.isPinned) - Number(a.isPinned)))
          }}
        />
      )}

      {isLoading ? (
        <p className="text-sm text-ink-500">Loading…</p>
      ) : posts.length === 0 ? (
        <Card>
          <EmptyState
            title="No announcements yet"
            description="Be the first to share an update with the company."
            icon={<Megaphone className="h-5 w-5" />}
          />
        </Card>
      ) : (
        <div className="flex flex-col gap-4">
          {posts.map((post) => (
            <PostCard key={post.id} post={post} onDelete={() => handleDeletePost(post.id)} />
          ))}
        </div>
      )}
    </div>
  )
}

function Composer({
  canPin,
  onClose,
  onCreated,
}: {
  canPin: boolean
  onClose: () => void
  onCreated: (post: AnnouncementResponse) => void
}) {
  const [title, setTitle] = useState('')
  const [body, setBody] = useState('')
  const [isPinned, setIsPinned] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      const post = await announcementsApi.create({ title, body, isPinned: canPin ? isPinned : undefined })
      onCreated(post)
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Failed to publish post')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Card>
      <CardHeader
        title="New announcement"
        action={
          <button onClick={onClose} className="text-ink-400 hover:text-ink-600">
            <X className="h-5 w-5" />
          </button>
        }
      />
      {error && (
        <div className="mb-4">
          <Alert variant="error">{error}</Alert>
        </div>
      )}
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <Input label="Title" required value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Q3 roadmap update" />
        <Textarea
          label="Message"
          required
          value={body}
          onChange={(e) => setBody(e.target.value)}
          placeholder="Share the details…"
        />
        {canPin && (
          <label className="flex items-center gap-2 text-sm text-ink-700">
            <input type="checkbox" checked={isPinned} onChange={(e) => setIsPinned(e.target.checked)} />
            Pin to top of feed
          </label>
        )}
        <div className="flex items-center gap-3">
          <Button type="submit" isLoading={isSubmitting}>
            Publish
          </Button>
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
        </div>
      </form>
    </Card>
  )
}

function PostCard({ post, onDelete }: { post: AnnouncementResponse; onDelete: () => void }) {
  const [isExpanded, setIsExpanded] = useState(false)
  const [comments, setComments] = useState<CommentResponse[]>([])
  const [isLoadingComments, setIsLoadingComments] = useState(false)
  const [commentBody, setCommentBody] = useState('')
  const [isSubmittingComment, setIsSubmittingComment] = useState(false)
  const [commentCount, setCommentCount] = useState(post.commentCount)
  const [error, setError] = useState<string | null>(null)

  function toggleExpanded() {
    if (!isExpanded && comments.length === 0 && commentCount > 0) {
      setIsLoadingComments(true)
      announcementsApi
        .getComments(post.id)
        .then(setComments)
        .catch(() => setError('Failed to load comments'))
        .finally(() => setIsLoadingComments(false))
    }
    setIsExpanded((v) => !v)
  }

  async function handleAddComment(e: FormEvent) {
    e.preventDefault()
    if (!commentBody.trim()) return
    setIsSubmittingComment(true)
    setError(null)
    try {
      const comment = await announcementsApi.addComment(post.id, commentBody)
      setComments((prev) => [...prev, comment])
      setCommentCount((c) => c + 1)
      setCommentBody('')
    } catch (err) {
      setError(err instanceof HttpError ? err.message : 'Failed to post comment')
    } finally {
      setIsSubmittingComment(false)
    }
  }

  async function handleDeleteComment(commentId: number) {
    try {
      await announcementsApi.removeComment(post.id, commentId)
      setComments((prev) => prev.filter((c) => c.id !== commentId))
      setCommentCount((c) => Math.max(0, c - 1))
    } catch {
      setError('Failed to delete comment')
    }
  }

  return (
    <Card>
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-3">
          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-accent-100 text-xs font-semibold text-accent-700">
            {initialsOf(post.authorName)}
          </div>
          <div>
            <div className="flex items-center gap-2">
              <p className="text-sm font-medium text-ink-900">{post.authorName}</p>
              {post.authorJobTitle && <span className="text-xs text-ink-400">· {post.authorJobTitle}</span>}
            </div>
            <p className="text-xs text-ink-500">{timeAgo(post.createdAt)}</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          {post.isPinned && (
            <span className="flex items-center gap-1 rounded-full bg-warning-50 px-2.5 py-1 text-xs font-medium text-warning-700">
              <Pin className="h-3 w-3" /> Pinned
            </span>
          )}
          {post.canManage && (
            <button onClick={onDelete} className="text-ink-400 hover:text-danger-600">
              <Trash2 className="h-4 w-4" />
            </button>
          )}
        </div>
      </div>

      <h3 className="mt-4 text-base font-semibold text-ink-900">{post.title}</h3>
      <p className="mt-1.5 whitespace-pre-line text-sm leading-relaxed text-ink-700">{post.body}</p>

      {error && (
        <div className="mt-3">
          <Alert variant="error">{error}</Alert>
        </div>
      )}

      <button
        onClick={toggleExpanded}
        className="mt-4 flex items-center gap-1.5 text-sm font-medium text-ink-500 hover:text-ink-900"
      >
        <MessageSquare className="h-4 w-4" />
        {commentCount === 0 ? 'Comment' : `${commentCount} comment${commentCount === 1 ? '' : 's'}`}
      </button>

      {isExpanded && (
        <div className="mt-4 border-t border-ink-100 pt-4">
          {isLoadingComments ? (
            <p className="text-sm text-ink-500">Loading comments…</p>
          ) : (
            <div className="flex flex-col gap-3">
              {comments.map((c) => (
                <div key={c.id} className="flex items-start gap-2.5">
                  <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-ink-100 text-[10px] font-semibold text-ink-600">
                    {initialsOf(c.authorName)}
                  </div>
                  <div className="flex-1 rounded-lg bg-ink-50 px-3 py-2">
                    <div className="flex items-center justify-between gap-2">
                      <p className="text-xs font-medium text-ink-900">{c.authorName}</p>
                      <div className="flex items-center gap-2">
                        <span className="text-[11px] text-ink-400">{timeAgo(c.createdAt)}</span>
                        {c.canManage && (
                          <button onClick={() => handleDeleteComment(c.id)} className="text-ink-300 hover:text-danger-600">
                            <Trash2 className="h-3 w-3" />
                          </button>
                        )}
                      </div>
                    </div>
                    <p className="mt-0.5 text-sm text-ink-700">{c.body}</p>
                  </div>
                </div>
              ))}
            </div>
          )}

          <form onSubmit={handleAddComment} className="mt-3 flex items-center gap-2">
            <input
              value={commentBody}
              onChange={(e) => setCommentBody(e.target.value)}
              placeholder="Write a comment…"
              className="flex-1 rounded-lg border border-ink-300 bg-white px-3.5 py-2 text-sm text-ink-900 placeholder:text-ink-400 focus:border-accent-600"
            />
            <Button type="submit" size="sm" isLoading={isSubmittingComment} icon={<Send className="h-3.5 w-3.5" />}>
              Send
            </Button>
          </form>
        </div>
      )}
    </Card>
  )
}
