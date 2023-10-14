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
          <i class="icono-image" /><br />
          <small>Pictures</small>
        </a>
      </div>
      <div class="pv8">
        <a>
          <i class="icono-headphone" /><br />
          <small>Stems</small>
        </a>
      </div>
      <div class="pv8">
        <a>
          <i class="icono-video" /><br />
          <small>Videos</small>
        </a>
      </div>
      <div class="pv8">
        <a>
          <i class="icono-tiles" /><br />
          <small>Mixes</small>
        </a>
      </div>
    </div>
  </div>
)

export default A4MenuLeft
