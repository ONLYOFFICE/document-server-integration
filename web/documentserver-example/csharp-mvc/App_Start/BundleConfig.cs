/**
 *
 * (c) Copyright Ascensio System SIA 2024
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

using System.Web.Optimization;

namespace OnlineEditorsExampleMVC
{
    public class BundleConfig
    {
        // register bundles
        public static void RegisterBundles(BundleCollection bundles)
        {
            // create a jquery script bundle
            bundles.Add(new ScriptBundle("~/bundles/jquery").Include(
                "~/Scripts/jquery-{version}.js",
                "~/Scripts/jquery-migrate-{version}.js",
                "~/Scripts/jquery-ui.js",
                "~/Scripts/jquery.blockUI.js",
                "~/Scripts/jquery.iframe-transport.js",
                "~/Scripts/jquery.fileupload.js",
                "~/Scripts/jquery.dropdownToggle.js"
                            ));

            // create the main script bundle
            bundles.Add(new ScriptBundle("~/bundles/scripts").Include(
                "~/Scripts/jscript.js"
                            ));

            // create a style bundle
            bundles.Add(new StyleBundle("~/Content/css").Include(
                "~/Content/stylesheet.css",
                "~/Content/jquery-ui.css",
                "~/Content/media.css"
                            ));

            // create an editor style bundle
            bundles.Add(new StyleBundle("~/Content/editor").Include(
                "~/Content/editor.css"
                            ));
        }
    }
}