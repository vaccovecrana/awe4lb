import * as React from "preact/compat"

import A4Logo from "./A4Logo"

const A4MenuLeft = () => (
  <div id="menuLeft">
    <div class="txc">
      <div class="logo mv16">
        <A4Logo />
      </div>
      <div class="pv8">
        <a>
          <i class="icono-areaChart" /><br />
          <small>Status</small>
        </a>
      </div>
      <div class="pv8">
        <a>
          <i class="icono-tiles" /><br />
          <small>Configurations</small>
        </a>
      </div>
      <div class="pv8">
        <a>
          <i class="icono-rename" /><br />
          <small>Editor</small>
        </a>
      </div>
    </div>
  </div>
)

export default A4MenuLeft
