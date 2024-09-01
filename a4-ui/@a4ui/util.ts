import { A4StringOp, A4MatchOp, A4Match, A4Server } from "@a4ui/rpc";

export const uiRoot = "/"
export const uiConfigList = "/config/list"
export const uiConfigEdit = "/config/:configId/edit"

export const uiConfigEditFmt = (configId: string) => `/config/${configId}/edit`

const stringOpLabelOf = (op: A4StringOp): string => {
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

const matchOpLabelOf = (op: A4MatchOp): string => {
  const varId =
    op.host ? "host" :
    op.sni ? "sni" : "?"
  const varVal = stringOpLabelOf(op.host || op.sni)
  return `${varId} ${varVal}`
}

export const matchLabelOf = (match: A4Match): string => {
  if (match.and === undefined && match.or === undefined) {
    return "any"
  }
  const sep = match.and ? "and" : "or"
  const vals = (match.and || match.or).map(matchOpLabelOf)
  return vals.join(sep)
}

export const serverTypeOf = (srv: A4Server) => {
  if (srv.udp) {
    return "UDP"
  }
  if (srv.tls) {
    return "TLS"
  }
  return "TCP"
}
