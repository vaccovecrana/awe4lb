import * as React from "preact/compat"

import { uiConfigList, uiRoot } from "@a4ui/util"
import { A4Config, A4Logo, A4Status } from "./A4Icons"

const A4MenuLeft = () => (
  <div class="p16">
    <div class="txc">
      <div class="logo mv16">
        <A4Logo maxHeight={42} />
      </div>
      <div class="m8 pt8">
        <a class="btn small secondary block" href={uiRoot}>
          <A4Status maxHeight={24} /> Status
        </a>
      </div>
      <div class="m8 pt8">
        <a class="btn small secondary block" href={uiConfigList}>
          <A4Config maxHeight={24} /> Configs
        </a>
      </div>
    </div>
  </div>
)

export default A4MenuLeft
