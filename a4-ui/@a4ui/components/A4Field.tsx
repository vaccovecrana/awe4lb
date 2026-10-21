import {RenderableProps} from "preact"

export const A4Field = (props: RenderableProps<{
  label: string
  hint?: string
}>) => (
  <div class="a4-field">
    <span class="a4-form-label">{props.label}</span>
    {props.children}
    {props.hint && <div class="a4-form-text">{props.hint}</div>}
  </div>
)

export default A4Field
