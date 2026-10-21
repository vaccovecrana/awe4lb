import {statusClass} from "@a4ui/util"

const A4StatusPill = (props: { state?: string }) => (
  <span class={statusClass(props.state)}>{props.state || "Unknown"}</span>
)

export default A4StatusPill
