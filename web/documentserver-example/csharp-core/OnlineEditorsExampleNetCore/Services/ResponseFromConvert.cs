using System;
using System.Collections.Generic;
using System.Linq;
using System.Text.Json.Serialization;
using System.Threading.Tasks;

namespace OnlineEditorsExampleNetCore.Services
{
    public class ResponseFromConvert
    {
        [JsonPropertyName("error")]
        public string Error { get; set; }

        [JsonPropertyName("endConvert")]
        public bool EndConvert { get; set; }

        [JsonPropertyName("fileUrl")]
        public string FileUrl { get; set; }

        [JsonPropertyName("percent")]
        public int Percent { get; set; }
    }
}
