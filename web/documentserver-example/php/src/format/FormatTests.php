<?php
//
// (c) Copyright Ascensio System SIA 2023
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

namespace Example\Format\Tests;

use PHPUnit\Framework\TestCase;
use Symfony\Component\Serializer\Encoder\JsonEncoder;
use Symfony\Component\Serializer\Normalizer\ObjectNormalizer;
use Symfony\Component\Serializer\Serializer;
use Example\Format\Format;

final class FormatTests extends TestCase
{
    public string $json =
    '
    {
      "name": "djvu",
      "type": "word",
      "actions": ["view"],
      "convert": ["bmp", "gif", "jpg", "pdf", "pdfa", "png"],
      "mime": ["image/vnd.djvu"]
    }
    ';

    public function testGeneratesExtension()
    {
        $serializer = new Serializer(
            [
                new ObjectNormalizer()
            ],
            [
                new JsonEncoder()
            ]
        );
        $format = $serializer->deserialize($this->json, Format::class, 'json');
        $this->assertEquals('djvu', $format->extension());
    }
}
