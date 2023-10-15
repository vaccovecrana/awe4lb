import * as React from "preact/compat"
import A4Logo from "./A4Logo"

const A4MenuTop = () => (
  <div class="pt8">
    <div class="row justify-center align-center">
      <div class="col auto">
        <div class="txc logo">
          <A4Logo />
        </div>
      </div>
      <div class="col auto">
        <div class="txc">
          <div class="pv8">
            <a>
              <i class="icono-areaChart" /><br />
              <small>Status</small>
            </a>
          </div>
        </div>
      </div>
      <div class="col auto">
        <div class="txc">
          <div class="pv8">
            <a>
              <i class="icono-tiles" /><br />
              <small>Configurations</small>
            </a>
          </div>
        </div>
      </div>
      <div class="col auto">
        <div class="txc">
          <div class="pv8">
            <a>
              <i class="icono-rename" /><br />
              <small>Editor</small>
            </a>
          </div>
        </div>
      </div>
    </div>
  </div>
)

export default A4MenuTop
