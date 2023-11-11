import * as React from "preact/compat"
import A4Logo from "./A4Logo"
import { uiConfigList, uiRoot } from "@a4ui/routes"

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
            <a href={uiRoot}>
              <i class="icon-layers" /><br />
              <small>Status</small>
            </a>
          </div>
        </div>
      </div>
      <div class="col auto">
        <div class="txc">
          <div class="pv8">
            <a href={uiConfigList}>
              <i class="icon-grid" /><br />
              <small>Configurations</small>
            </a>
          </div>
        </div>
      </div>
      <div class="col auto">
        <div class="txc">
          <div class="pv8">
            <a>
              <i class="icon-pencil" /><br />
              <small>Editor</small>
            </a>
          </div>
        </div>
      </div>
    </div>
  </div>
)

export default A4MenuTop
