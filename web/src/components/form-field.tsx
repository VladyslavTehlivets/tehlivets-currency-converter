import { Label } from '@/components/ui/label'

export function errorIdFor(fieldId: string) {
  return `${fieldId}-error`
}

export function describedByError(fieldId: string, error: string | undefined) {
  return error ? { 'aria-invalid': true, 'aria-describedby': errorIdFor(fieldId) } : {}
}

export function FormField({
  fieldId,
  label,
  error,
  children,
}: {
  fieldId: string
  label: string
  error: string | undefined
  children: React.ReactNode
}) {
  return (
    <div className="mb-5 grid gap-2 text-left">
      <Label htmlFor={fieldId}>{label}</Label>
      {children}
      {error ? (
        <p id={errorIdFor(fieldId)} className="text-caption text-error">
          {error}
        </p>
      ) : null}
    </div>
  )
}

export function FormError({ message }: { message: string | null }) {
  return (
    <p role="alert" className="my-3 text-caption text-error empty:my-0">
      {message}
    </p>
  )
}
