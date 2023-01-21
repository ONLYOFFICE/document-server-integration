/**
 *
 * (c) Copyright Ascensio System SIA 2023
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

package helpers;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class CookieManager {
    private HashMap<String, String> cookiesMap;

    public CookieManager(final HttpServletRequest request) throws UnsupportedEncodingException {
        cookiesMap = new HashMap<String, String>();

        Cookie[] cookies = request.getCookies();  // get all the cookies from the request
        if (cookies != null) {
            for (Cookie cookie : cookies) {  // run through all the cookies

                // add cookie to the cookies map if its name isn't in the map yet
                cookiesMap.putIfAbsent(cookie.getName(), URLDecoder.decode(cookie.getValue(), "UTF-8"));
            }
        }
    }

    // get cookie by its name
    public String getCookie(final String name) {
        return cookiesMap.get(name);
    }
}
