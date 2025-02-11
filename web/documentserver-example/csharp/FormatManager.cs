/**
 *
 * (c) Copyright Ascensio System SIA 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using Newtonsoft.Json;

namespace OnlineEditorsExample
{
    public class Format
    {
        public string Name { get; }
        public string Type { get; }
        public List<string> Actions { get; }
        public List<string> Convert { get; }
        public List<string> Mime { get; }

        public Format(string name, string type, List<string> actions, List<string> convert, List<string> mime)
        {
            Name = name;
            Type = type;
            Actions = actions;
            Convert = convert;
            Mime = mime;
        }

        public string Extension()
        {
            return "." + Name;
        }
    }

    public class FormatManager
    {
        private static List<Format> cachedFormats;

        public static List<string> FillableExtensions()
        {
            return Fillable()
                .Select(format => format.Extension())
                .ToList();
        }

        public static List<Format> Fillable()
        {
            return All()
                .Where(format => format.Actions.Contains("fill"))
                .ToList();
        }

        public static List<string> ViewableExtensions()
        {
            return Viewable()
                .Select(format => format.Extension())
                .ToList();
        }

        public static List<Format> Viewable()
        {
            return All()
                .Where(format => format.Actions.Contains("view"))
                .ToList();
        }

        public static List<string> EditableExtensions()
        {
            return Editable()
                .Select(format => format.Extension())
                .ToList();
        }

        public static List<Format> Editable()
        {
            return All()
                .Where(format => format.Actions.Contains("edit") || format.Actions.Contains("lossy-edit"))
                .ToList();
        }

        public static List<string> ConvertibleExtensions()
        {
            return Convertible()
                .Select(format => format.Extension())
                .ToList();
        }

        public static List<Format> Convertible()
        {
            return All()
                .Where(format => format.Actions.Contains("auto-convert"))
                .ToList();
        }

        public static List<string> SpreadsheetExtensions()
        {
            return Spreadsheets()
                .Select(format => format.Extension())
                .ToList();
        }

        public static List<Format> Spreadsheets()
        {
            return All()
                .Where(format => format.Type == "cell")
                .ToList();
        }

        public static List<string> PresentationExtensions()
        {
            return Presentations()
                .Select(format => format.Extension())
                .ToList();
        }

        public static List<Format> Presentations()
        {
            return All()
                .Where(format => format.Type == "slide")
                .ToList();
        }

        public static List<string> DocumentExtensions()
        {
            return Documents()
                .Select(format => format.Extension())
                .ToList();
        }

        public static List<Format> Documents()
        {
            return All()
                .Where(format => format.Type == "word")
                .ToList();
        }

        public static List<string> PdfExtensions()
        {
            return Pdfs()
                .Select(format => format.Extension())
                .ToList();
        }

        public static List<Format> Pdfs()
        {
            return All()
                .Where(format => format.Type == "pdf")
                .ToList();
        }

        public static List<string> DiagramExtensions()
        {
            return Diagrams()
                .Select(format => format.Extension())
                .ToList();
        }

        public static List<Format> Diagrams()
        {
            return All()
                .Where(format => format.Type == "diagram")
                .ToList();
        }

        public static List<string> AllExtensions()
        {
            return All()
                .Select(format => format.Extension())
                .ToList();
        }

        public static List<Format> All()
        {
            if (cachedFormats == null) { 
                var path = GetPath();
                var lines = File.ReadLines(path, Encoding.UTF8);
                var contents = string.Join(Environment.NewLine, lines);
                var formats = JsonConvert.DeserializeObject<Format[]>(contents);
                cachedFormats = formats.ToList();
            }

            return cachedFormats;
        }

        private static string GetPath()
        {
            string path = Path.Combine(GetDirectory(), "onlyoffice-docs-formats.json");
            if (File.Exists(path))
            {
                return path;
            }
            else
            {
                throw new FileNotFoundException("The JSON file does not exist.");
            }
        }

        private static string GetDirectory()
        {
            string directory = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "assets", "document-formats");
            return Path.GetFullPath(directory);
        }
    }
}