using Minio.DataModel;
using System;
using System.IO;

namespace OnlineEditorsExampleMVC.Models
{
    public class FileInfoModel
    {
        public string Name { get; set; }
        public DateTime LastModified { get; set; }
        public long Length { get; set; }

        public static FileInfoModel FromFileInfo(FileInfo info)
        {
            return new FileInfoModel()
            {
                Name = info.Name,
                LastModified = info.LastWriteTime,
                Length = info.Length
            };
        }

        public static FileInfoModel FromS3(Item info)
        {
            return new FileInfoModel()
            {
                Name = info.Key,
                LastModified = info.LastModifiedDateTime.Value,
                Length = (long)info.Size
            };
        }
    }
}