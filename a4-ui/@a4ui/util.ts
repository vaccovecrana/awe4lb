import { A4StringOp, A4MatchOp, A4Match } from "@a4ui/rpc";

export const uiRoot = "/"
export const uiConfigList = "/config/list"
export const uiConfigEdit = "/config/:configId/edit"

const stringOpLabel = (op: A4StringOp): string => {
  if (!op) {
    return "?"
  }
  const opId =
    op.contains ? "contains" :
    op.endsWith ? "endsWith" :
    op.equals   ? "equals"   :
    op.startsWith ? "startsWith" :
    "?"
  const opVal = 
    op.contains ? op.contains :
    op.endsWith ? op.endsWith :
    op.equals ? op.equals :
    op.startsWith ? op.startsWith :
    "?"
  return `${opId}(${opVal})`
}

const matchOpLabel = (op: A4MatchOp): string => {
  const varId =
    op.host ? "host" :
    op.sni ? "sni" : "?"
  const varVal = stringOpLabel(op.host || op.sni)
  return `${varId} ${varVal}`
}

export const matchLabel = (match: A4Match): string => {
  const sep = match.and ? "and" : "or"
  const vals = (match.and || match.or).map(matchOpLabel)
  return vals.join(sep)
}
