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
              <i class="icono-image" /><br />
              <small>Pictures</small>
            </a>
          </div>
        </div>
      </div>
      <div class="col auto">
        <div class="txc">
          <div class="pv8">
            <a>
              <i class="icono-headphone" /><br />
              <small>Stems</small>
            </a>
          </div>
        </div>
      </div>
      <div class="col auto">
        <div class="txc">
          <div class="pv8">
            <a>
              <i class="icono-video" /><br />
              <small>Videos</small>
            </a>
          </div>
        </div>
      </div>
      <div class="col auto">
        <div class="txc">
          <div class="pv8">
            <a>
              <i class="icono-tiles" /><br />
              <small>Mixes</small>
            </a>
          </div>
        </div>
      </div>
    </div>
  </div>
)

export default A4MenuTop
