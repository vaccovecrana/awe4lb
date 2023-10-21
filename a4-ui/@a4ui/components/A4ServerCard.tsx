import * as React from "preact/compat"
import { A4Server } from "@a4ui/rpc"
import A4MatchCard from "./A4MatchCard"

interface A4ScProps { srv: A4Server }

const A4ServerCard = (props: A4ScProps) => (
  <div class="col xs-12 sm-12 md-6">
    <div class="card minimal p8 m2 mt8">
      <div class="card-title-2">
        <i class="icon-compass" /> {props.srv.id}
      </div>
      {props.srv.tls ? (
        <div class="mt8 p8 txSmall card minimal">
          <div class="card-title-3">TLS</div>
          <div class="mt8">
            <i class="icon-doc" /> <span>{props.srv.tls.certPath}</span>
          </div>
          <div>
            <i class="icon-key" /> <span>{props.srv.tls.keyPath}</span>
          </div>
          {props.srv.tls.ciphers && props.srv.tls.ciphers.length > 0 ? (
            <div>
              <i class="icon-list" /> {props.srv.tls.ciphers.join(", ")}
            </div>
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
