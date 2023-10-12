import typescript from "@rollup/plugin-typescript"
import nodeResolve from "@rollup/plugin-node-resolve"
import scss from "rollup-plugin-scss"

export default {
  input: "./src/main/javascript/index.tsx",
  output: {dir: "./build/ui", format: "esm", sourcemap: true},
  plugins: [
    nodeResolve(),
    typescript(),
    scss({output: "./build/ui/index.css", failOnError: true})
  ]
}
