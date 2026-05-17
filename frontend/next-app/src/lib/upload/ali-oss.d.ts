declare module "ali-oss" {
  type OssClientOptions = {
    region: string;
    endpoint?: string;
    accessKeyId: string;
    accessKeySecret: string;
    stsToken: string;
    bucket: string;
  };

  type PutOptions = {
    timeout?: number;
  };

  type PutResult = {
    name: string;
    url: string;
  };

  export default class OSS {
    constructor(options: OssClientOptions);
    put(name: string, file: Blob | File, options?: PutOptions): Promise<PutResult>;
  }
}
