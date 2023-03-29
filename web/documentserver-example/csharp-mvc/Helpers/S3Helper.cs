using Minio;
using Minio.DataModel;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reactive.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Threading.Tasks;
using System.Web.Configuration;

namespace OnlineEditorsExampleMVC.Helpers
{
    public class S3Helper
    {
        private S3Helper()
        {
            bool.TryParse(WebConfigurationManager.AppSettings["s3-enabled"], out var enable);
            if (enable)
            {
                MinioClient = new MinioClient()
                    .WithEndpoint(WebConfigurationManager.AppSettings["s3-endpoint"])
                    .WithCredentials(WebConfigurationManager.AppSettings["s3-access-key"], WebConfigurationManager.AppSettings["s3-secret-key"])
                    .WithRegion(WebConfigurationManager.AppSettings["s3-region"]);

                if (bool.Parse(WebConfigurationManager.AppSettings["s3-secure"]))
                {
                    MinioClient.WithSSL();
                }

                MinioClient.Build();
            }
        }

        private static S3Helper instance;
        public static S3Helper Instance
        {
            get
            {
                if (instance == null)
                {
                    instance = new S3Helper();
                }
                return instance;
            }
        }

        private string Bucket { get { return WebConfigurationManager.AppSettings["s3-bucket"]; } }
        private MinioClient MinioClient { get; set; }

        public bool Enabled { get { return MinioClient != null; } }

        public List<Item> ListAllItemsSync(string prefix = null)
        {
            return AwaitTaskSync(() => ListAllItems(prefix));
        }

        public void UploadFileSync(string name, string path)
        {
            AwaitTaskSync(() => UploadFile(name, path));
        }

        public void UploadFileSync(string name, Stream stream)
        {
            AwaitTaskSync(() => UploadFile(name, stream));
        }

        public void RemoveFilesSync(IEnumerable<string> names)
        {
            AwaitTaskSync(() => RemoveFiles(names));
        }

        public Stream DownloadFileSync(string name)
        {
            return AwaitTaskSync(() => DownloadFile(name));
        }

        public void CopyFileSync(string oldname, string newname)
        {
            AwaitTaskSync(() => CopyFile(oldname, newname));
        }

        private async Task<List<Item>> ListAllItems(string prefix = null)
        {
            var list = new List<Item>();
            var shouldWait = true;
            var args = new ListObjectsArgs().WithBucket(Bucket).WithRecursive(false);
            if (!string.IsNullOrWhiteSpace(prefix))
            {
                args.WithPrefix(prefix).WithRecursive(true);
            }
            var observable = MinioClient.ListObjectsAsync(args);
            var subscription = observable.Subscribe(item => list.Add(item), () => shouldWait = false);
            while (shouldWait)
            {
                await Task.Delay(100);
            }

            return list;
        }

        private async Task UploadFile(string name, string path)
        {
            using (var stream = File.OpenRead(path))
            {
                await UploadFile(name, stream);
            }
        }

        private async Task UploadFile(string name, Stream stream)
        {
            await MinioClient.PutObjectAsync(new PutObjectArgs().WithBucket(Bucket).WithObject(name).WithObjectSize(stream.Length).WithStreamData(stream));
        }

        private async Task RemoveFiles(IEnumerable<string> names)
        {
            await MinioClient.RemoveObjectsAsync(new RemoveObjectsArgs().WithBucket(Bucket).WithObjects(names.ToList()));
        }

        private async Task CopyFile(string oldname, string newname)
        {
            await MinioClient.CopyObjectAsync(new CopyObjectArgs().WithBucket(Bucket).WithObject(newname)
                .WithCopyObjectSource(new CopySourceObjectArgs().WithBucket(Bucket).WithObject(oldname)));
        }

        private async Task<Stream> DownloadFile(string name)
        {
            Stream stream = null;
            var shouldWait = true;
            await MinioClient.GetObjectAsync(new GetObjectArgs().WithBucket(Bucket).WithObject(name).WithCallbackStream((s) =>
            {
                stream = s;
                shouldWait = false;
            }));
            while (shouldWait)
            {
                await Task.Delay(100);
            }

            return stream;
        }

        private void AwaitTaskSync(Func<Task> f)
        {
            Task.Run(() => f()).GetAwaiter().GetResult();
        }

        private T AwaitTaskSync<T>(Func<Task<T>> f)
        {
            return Task.Run(() => f()).GetAwaiter().GetResult();
        }

        public static Stream StringToStream(string content)
        {
            return new MemoryStream(Encoding.UTF8.GetBytes(content ?? ""));
        }
    }
}