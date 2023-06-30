#
# (c) Copyright Ascensio System SIA 2023
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

from __future__ import annotations
from dataclasses import dataclass
from textwrap import dedent
from typing import Optional
from unittest import TestCase
from . import Codable, CodingKey

@dataclass
class Fruit(Codable):
    class CodingKeys(CodingKey):
        name = 'fruit_name'
        weight = 'fruitWeight'
        texture = 'fruit_texture'
        vitamins = 'fruitVitamins'
        organic = 'fruit_organic'

    name: str
    weight: int
    texture: Optional[str]
    vitamins: list[str]
    organic: bool

class CodablePlainTests(TestCase):
    json = (
        dedent(
            '''
            {
              "fruit_name": "kiwi",
              "fruitWeight": 100,
              "fruit_texture": null,
              "fruitVitamins": [
                "Vitamin C",
                "Vitamin K"
              ],
              "fruit_organic": true
            }
            '''
        )
            .strip()
    )

    def test_decodes(self):
        fruit = Fruit.decode(self.json)
        self.assertEqual(fruit.name, 'kiwi')
        self.assertEqual(fruit.weight, 100)
        self.assertIsNone(fruit.texture)
        self.assertEqual(fruit.vitamins, ['Vitamin C', 'Vitamin K'])
        self.assertTrue(fruit.organic)

    def test_encodes(self):
        fruit = Fruit(
            name='kiwi',
            weight=100,
            texture=None,
            vitamins=['Vitamin C', 'Vitamin K'],
            organic=True
        )
        content = fruit.encode()
        self.assertEqual(content, self.json)

@dataclass
class Smoothie(Codable):
    class CodingKeys(CodingKey):
        recipe = 'recipe'

    recipe: Recipe

@dataclass
class Recipe(Codable):
    class CodingKeys(CodingKey):
        ingredients = 'ingredients'

    ingredients: list[Ingredient]

@dataclass
class Ingredient(Codable):
    class CodingKeys(CodingKey):
        name = 'name'

    name: str

class CodableNestedTests(TestCase):
    json = (
        dedent(
            '''
            {
              "recipe": {
                "ingredients": [
                  {
                    "name": "kiwi"
                  }
                ]
              }
            }
            '''
        )
            .strip()
    )

    def test_decodes(self):
        smoothie = Smoothie.decode(self.json)
        self.assertEqual(smoothie.recipe.ingredients[0].name, 'kiwi')

    def test_encodes(self):
        ingredient = Ingredient(name='kiwi')
        recipe = Recipe(ingredients=[ingredient])
        smoothie = Smoothie(recipe=recipe)
        content = smoothie.encode()
        self.assertEqual(content, self.json)

@dataclass
class Vegetable(Codable):
    class CodingKeys(CodingKey):
        name = 'name'

    name: Optional[str]

class CodableMissedTests(TestCase):
    source_json = '{}'
    distribute_json = (
        dedent(
            '''
            {
              "name": null
            }
            '''
        )
            .strip()
    )

    def test_decodes(self):
        vegetable = Vegetable.decode(self.source_json)
        self.assertIsNone(vegetable.name)

    def test_encodes(self):
        vegetable = Vegetable(name=None)
        content = vegetable.encode()
        self.assertEqual(content, self.distribute_json)
