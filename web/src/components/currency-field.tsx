import { describedByError, FormField } from '@/components/form-field'
import { NativeSelect } from '@/components/ui/native-select'

export function CurrencyField({
  fieldId,
  label,
  choices,
  value,
  error,
  onChoose,
}: {
  fieldId: string
  label: string
  choices: string[]
  value: string
  error: string | undefined
  onChoose: (currency: string) => void
}) {
  return (
    <FormField fieldId={fieldId} label={label} error={error}>
      <NativeSelect
        id={fieldId}
        name={fieldId}
        required
        value={value}
        onChange={(event) => onChoose(event.target.value)}
        {...describedByError(fieldId, error)}
      >
        {choices.map((currency) => (
          <option key={currency} value={currency}>
            {currency}
          </option>
        ))}
      </NativeSelect>
    </FormField>
  )
}
