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

export interface A4Tls {
  certPath: string;
  keyPath: string;
  protocols: string[];
  ciphers: string[];
  
  
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

export interface Random {
  
  
}

export interface A4Pool {
  type: Type;
  hosts: A4Backend[];
  openTls: boolean;
  rnd: Random;
  rrVal: number;
  
  
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

export interface A4Server {
  id: string;
  addr: A4Sock;
  tls: A4Tls;
  match: A4Match[];
  
  
}

export interface A4Config {
  Seed: number;
  id: string;
  description: string;
  api: A4Sock;
  servers: A4Server[];
  
  
}


/* ====================================== */
/* ============ RPC methods ============= */
/* ====================================== */

/*
Source controllers:

- io.vacco.a4lb.web.A4ApiHdl

 */

export const getActiveConfig = (): Promise<A4Config> => {
  let path = "/api/v1/config"
  
  
  
  
  return doJsonIo(path, "GET",
    
      undefined
    ,
    new Map(),
    undefined
  )
}

