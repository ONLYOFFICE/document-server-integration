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

namespace Example\Format;

use Symfony\Component\Serializer\Encoder\JsonEncoder;
use Symfony\Component\Serializer\Normalizer\ArrayDenormalizer;
use Symfony\Component\Serializer\Normalizer\ObjectNormalizer;
use Symfony\Component\Serializer\Serializer;
use Example\Common\Path;

class FormatManager
{
    private Serializer $serializer;

    public function __construct()
    {
        $this->serializer = new Serializer(
            [
                new ArrayDenormalizer(),
                new ObjectNormalizer()
            ],
            [
                new JsonEncoder()
            ]
        );
    }

    /**
     * @return string[]
     */
    public function fillableExtensions(): array
    {
        $formats = $this->fillable();
        $extensions = [];
        foreach ($formats as $format) {
            $extensions[] = $format->extension();
        }
        return $extensions;
    }

    /**
     * @return Format[]
     */
    public function fillable(): array
    {
        $formats = $this->all();
        $filtered = [];
        foreach ($formats as $format) {
            if (in_array('fill', $format->actions)) {
                $filtered[] = $format;
            }
        }
        return $filtered;
    }

    /**
     * @return string[]
     */
    public function viewableExtensions(): array
    {
        $formats = $this->viewable();
        $extensions = [];
        foreach ($formats as $format) {
            $extensions[] = $format->extension();
        }
        return $extensions;
    }

    /**
     * @return Format[]
     */
    public function viewable(): array
    {
        $formats = $this->all();
        $filtered = [];
        foreach ($formats as $format) {
            if (in_array('view', $format->actions)) {
                $filtered[] = $format;
            }
        }
        return $filtered;
    }

    /**
     * @return string[]
     */
    public function editableExtensions(): array
    {
        $formats = $this->editable();
        $extensions = [];
        foreach ($formats as $format) {
            $extensions[] = $format->extension();
        }
        return $extensions;
    }

    /**
     * @return Format[]
     */
    public function editable(): array
    {
        $formats = $this->all();
        $filtered = [];
        foreach ($formats as $format) {
            if (in_array('edit', $format->actions) or
                in_array('lossy-edit', $format->actions)
            ) {
                $filtered[] = $format;
            }
        }
        return $filtered;
    }

    /**
     * @return string[]
     */
    public function convertibleExtensions(): array
    {
        $formats = $this->convertible();
        $extensions = [];
        foreach ($formats as $format) {
            $extensions[] = $format->extension();
        }
        return $extensions;
    }

    /**
     * @return Format[]
     */
    public function convertible(): array
    {
        $formats = $this->all();
        $filtered = [];
        foreach ($formats as $format) {
            if ($format->type === 'cell' and in_array('xlsx', $format->convert) or
                $format->type === 'slide' and in_array('pptx', $format->convert) or
                $format->type === 'word' and in_array('docx', $format->convert)
            ) {
                $filtered[] = $format;
            }
        }
        return $filtered;
    }

    /**
     * @return string[]
     */
    public function allExtensions(): array
    {
        $formats = $this->all();
        $extensions = [];
        foreach ($formats as $format) {
            $extensions[] = $format->extension();
        }
        return $extensions;
    }

    /**
     * @return Format[]
     */
    public function all(): array
    {
        $file = $this->file();
        $contents = $file->contents();
        return $this->serializer->deserialize(
            $contents,
            Format::class . '[]',
            'json'
        );
    }

    private function file(): Path
    {
        $directory = $this->directory();
        return $directory->joinPath('onlyoffice-docs-formats.json');
    }

    private function directory(): Path
    {
        $currentDirectory = new Path(__DIR__);
        return $currentDirectory
            ->joinPath('..')
            ->joinPath('..')
            ->joinPath('assets')
            ->joinPath('document-formats');
    }
}
