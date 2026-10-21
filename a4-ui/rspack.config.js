import path from "node:path"
import {fileURLToPath} from "node:url"

const root = path.dirname(fileURLToPath(import.meta.url))

export default {
  entry: "./@a4ui/index.tsx",
  devtool: "source-map",
  output: {
    filename: "index.js",
    path: path.resolve(root, "build", "ui"),
    clean: true,
  },
  resolve: {
    extensions: [".ts", ".tsx", ".js", ".jsx"],
    alias: {
      react: "preact/compat",
      "react-dom": "preact/compat",
      "react/jsx-runtime": "preact/jsx-runtime",
      "@a4ui": path.resolve(root, "./@a4ui"),
    }
  },
  module: {
    rules: [
      {
        test: /\.(ts|tsx)$/,
        exclude: /node_modules/,
        use: {
          loader: "builtin:swc-loader",
          options: {
            jsc: {
              parser: {syntax: "typescript", tsx: true},
              transform: {react: {runtime: "automatic"}}
            }
          }
        }
      }
    ]
  }
}
