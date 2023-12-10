/* ========================================================= */
/* ======== Generated file - do not modify directly ======== */
/* ========================================================= */

const doJsonIo = <I, O>(url: string, method: string, body: I,
                        headers: Map<string, string>, mediaType?: string): Promise<O> => {
  const options: any = {method, headers: {}}
  if (mediaType) {
    options.headers["Content-Type"] = mediaType
  }
  if (body) {
    options.body = body
  }
  headers.forEach((v, k) => options.headers[k] = v)
  return fetch(url, options)
    .then(response => response.json())
    .then(jData => Promise.resolve(jData as O))
}

/* ====================================== */
/* ============= RPC types ============== */
/* ====================================== */

export interface A4Sock {
  host: string;
  port: number;
  
  
}

export interface A4StringOp {
  equals: string;
  contains: string;
  startsWith: string;
  endsWith: string;
  
  
}

export interface A4MatchOp {
  sni: A4StringOp;
  host: A4StringOp;
  
  
}

export const enum Type {
  
  roundRobin = "roundRobin",
  leastConn = "leastConn",
  ipHash = "ipHash",
  weight = "weight",
  
}

export const enum State {
  
  Up = "Up",
  Down = "Down",
  Unknown = "Unknown",
  
}

export interface A4Backend {
  addr: A4Sock;
  weight: number;
  priority: number;
  state: State;
  
  
}

export interface A4Pool {
  type: Type;
  hosts: A4Backend[];
  openTls: boolean;
  
  
}

export const enum A4Format {
  
  text = "text",
  json = "json",
  
}

export interface A4DiscHttp {
  endpoint: string;
  format: A4Format;
  
  
}

export interface A4DiscExec {
  command: string;
  args: string[];
  format: A4Format;
  
  
}

export interface A4Disc {
  DefaultIntervalMs: number;
  DefaultTimeoutMs: number;
  http: A4DiscHttp;
  exec: A4DiscExec;
  intervalMs: number;
  timeoutMs: number;
  
  
}

export interface A4HealthExec {
  command: string;
  args: string[];
  
  
}

export interface A4HealthCheck {
  DefaultIntervalMs: number;
  DefaultTimeoutMs: number;
  intervalMs: number;
  timeoutMs: number;
  exec: A4HealthExec;
  
  
}

export interface A4Match {
  and: A4MatchOp[];
  or: A4MatchOp[];
  pool: A4Pool;
  discover: A4Disc;
  healthCheck: A4HealthCheck;
  
  
}

export interface A4Tls {
  certPath: string;
  keyPath: string;
  protocols: string[];
  ciphers: string[];
  
  
}

export interface A4Udp {
  bufferSize: number;
  idleTimeoutMs: number;
  maxSessions: number;
  
  
}

export interface A4Server {
  id: string;
  addr: A4Sock;
  match: A4Match[];
  tls: A4Tls;
  udp: A4Udp;
  
  
}

export interface A4Config {
  Seed: number;
  active: boolean;
  id: string;
  description: string;
  servers: A4Server[];
  
  
}

export interface A4Validation {
  args: string[];
  format: string;
  key: string;
  name: string;
  message: string;
  
  
}

export interface A4ConfigState {
  active: A4Config;
  inactive: A4Config;
  
  
}


/* ====================================== */
/* ============ RPC methods ============= */
/* ====================================== */

/*
Source controllers:

- io.vacco.a4lb.web.A4ApiHdl

 */

export const apiV1ConfigDelete = (configId: string): Promise<boolean> => {
  let path = "/api/v1/config"
  
  
    const qParams = new URLSearchParams()
    
      qParams.append("configId", configId.toString())
  
    
    path = `${path}?${qParams.toString()}`
  
  
  
  return doJsonIo(path, "DELETE",
    
      undefined
    ,
    new Map(),
    undefined
  )
}

export const apiV1ConfigGet = (configId: string): Promise<A4Config> => {
  let path = "/api/v1/config"
  
  
    const qParams = new URLSearchParams()
    
      qParams.append("configId", configId.toString())
  
    
    path = `${path}?${qParams.toString()}`
  
  
  
  return doJsonIo(path, "GET",
    
      undefined
    ,
    new Map(),
    undefined
  )
}

export const apiV1ConfigListGet = (): Promise<A4Config[]> => {
  let path = "/api/v1/config/list"
  
  
  
  
  return doJsonIo(path, "GET",
    
      undefined
    ,
    new Map(),
    undefined
  )
}

export const apiV1ConfigSelectGet = (configId: string): Promise<A4ConfigState> => {
  let path = "/api/v1/config/select"
  
  
    const qParams = new URLSearchParams()
    
      qParams.append("configId", configId.toString())
  
    
    path = `${path}?${qParams.toString()}`
  
  
  
  return doJsonIo(path, "GET",
    
      undefined
    ,
    new Map(),
    undefined
  )
}

export const apiV1ConfigPost = (configId: string, arg1: A4Config): Promise<A4Validation[]> => {
  let path = "/api/v1/config"
  
  
    const qParams = new URLSearchParams()
    
      qParams.append("configId", configId.toString())
  
    
    path = `${path}?${qParams.toString()}`
  
  
  
  return doJsonIo(path, "POST",
    
      JSON.stringify(arg1)
    ,
    new Map(),
    undefined
  )
}

