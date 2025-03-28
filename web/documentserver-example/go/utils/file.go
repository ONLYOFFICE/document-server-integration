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
package utils

import (
	"strings"
)

func GetFileName(str string) string {
	ind := strings.LastIndex(str, "/")
	return str[ind+1:]
}

func GetFileNameWithoutExt(str string) string {
	fn := GetFileName(str)
	ind := strings.LastIndex(fn, ".")
	return fn[:ind]
}

func GetFileExt(str string, withoutdot bool) string {
	pos := 0
	if withoutdot {
		pos = 1
	}
	fn := GetFileName(str)
	ind := strings.LastIndex(fn, ".")
	return strings.ToLower(fn[ind+pos:])
}
