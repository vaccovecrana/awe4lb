import * as React from "preact/compat"

import A4Logo from "./A4Logo"
import { uiConfigList, uiRoot } from "@a4ui/routes"

const A4MenuLeft = () => (
  <div id="menuLeft">
    <div class="txc">
      <div class="logo mv16">
        <A4Logo />
      </div>
      <div class="pv8">
        <a href={uiRoot}>
          <i class="icon-layers" /><br />
          <small>Status</small>
        </a>
      </div>
      <div class="pv8">
        <a href={uiConfigList}>
          <i class="icon-grid" /><br />
          <small>Configurations</small>
        </a>
      </div>
      <div class="pv8">
        <a>
          <i class="icon-pencil" /><br />
          <small>Editor</small>
        </a>
      </div>
    </div>
  </div>
)

export default A4MenuLeft
