import * as React from "preact/compat"
import { A4Server } from "@a4ui/rpc"
import A4MatchCard from "./A4MatchCard"

interface A4ScProps { srv: A4Server }

const A4ServerCard = (props: A4ScProps) => (
  <div class="col xs-12 sm-12 md-6">
    <div class="card minimal p8 m2 mt8">
      <div class="card-title-2 ph4">
        <div class="row">
          <div class="col auto">
            <i class="icon-compass mr4" /> {props.srv.id}
          </div>
          <div class="col auto">
            <div class="txr">
              <code>{props.srv.addr.host}:{props.srv.addr.port}/{props.srv.udp ? "udp" : props.srv.tls ? "tls" : "tcp"}</code>
            </div>
          </div>
        </div>
      </div>
      {props.srv.tls ? (
        <div class="mt8 p8 txSmall card minimal">
          <div class="card-title-3">TLS</div>
          <div class="mt4"><i class="icon-badge mr4" /><code>{props.srv.tls.certPath}</code></div>
          <div><i class="icon-key mr4" /><code>{props.srv.tls.keyPath}</code></div>
          {props.srv.tls.ciphers && props.srv.tls.ciphers.length > 0 ? (
            <div><i class="icon-list mr4" /><code>{props.srv.tls.ciphers.join(", ")}</code></div>
          ) : []}
        </div>
      ) : []}
      {props.srv.match.map((match, k) => [
        <A4MatchCard match={match} />,
        props.srv.match.length > 1 && (k < props.srv.match.length - 1) ? <hr class="mt8" /> : []
      ])}
    </div>
  </div>
)

export default A4ServerCard
