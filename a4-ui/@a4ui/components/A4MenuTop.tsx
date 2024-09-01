import * as React from "preact/compat"

import { uiConfigList, uiRoot } from "@a4ui/util"
import { A4Config, A4Logo, A4Status } from "./A4Icons"

const A4MenuTop = () => (
  <div class="mt16">
    <div class="p8">
      <div class="row justify-center align-center">
        <div class="col xs-2 sm-2 md-2 lg-2 xl-2">
          <div class="txc logo">
            <A4Logo maxHeight={64} />
          </div>
        </div>
        <div class="col auto">
          <div class="txc">
            <a href={uiRoot}>
              <A4Status maxHeight={42} />
            </a>
          </div>
        </div>
        <div class="col auto">
          <div class="txc">
            <a href={uiConfigList}>
              <A4Config maxHeight={42} />
            </a>
          </div>
        </div>
      </div>
    </div>
  </div>
)

export default A4MenuTop
