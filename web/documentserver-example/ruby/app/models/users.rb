# frozen_string_literal: true

#
# (c) Copyright Ascensio System SIA 2025
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

# Represents a user with various attributes
class User
  attr_accessor :id
  attr_accessor :name
  attr_accessor :email
  attr_accessor :group
  attr_accessor :review_groups
  attr_accessor :comment_groups
  attr_accessor :user_info_groups
  attr_accessor :favorite
  attr_accessor :denied_permissions
  attr_accessor :descriptions
  attr_accessor :templates
  attr_accessor :avatar
  attr_accessor :goback
  attr_accessor :close

  def initialize(
    id,
    name,
    email,
    group,
    review_groups,
    comment_groups,
    user_info_groups,
    favorite,
    denied_permissions,
    descriptions,
    templates,
    avatar,
    goback,
    close
  )
    @id = id
    @name = name
    @email = email
    @group = group
    @review_groups = review_groups
    @comment_groups = comment_groups
    @favorite = favorite
    @denied_permissions = denied_permissions
    @descriptions = descriptions
    @templates = templates
    @user_info_groups = user_info_groups
    @avatar = avatar
    @goback = goback
    @close = close
  end
end

# Manages user-related data and operations.
class Users
  @descr_user_first = [
    'File author by default',
    'Doesn’t belong to any group',
    'Can review all the changes',
    'Can perform all actions with comments',
    'Can see the information about all users',
    'This file isn’t marked as favorite',
    'Can create files from templates using data from the editor',
    'Has an avatar',
    'Can submit forms'
  ]

  @descr_user_second = [
    'Belongs to Group2',
    'Can review only his own changes or changes made by users with no group',
    'Can view comments, edit his own comments, and comments left by users with no group. ' \
    'Can remove his own comments only',
    'Can see the information about users from Group2 and users who don’t belong to any group',
    'This file is marked as favorite',
    'Can create new files from the editor',
    'Has an avatar',
    'Can’t submit forms'
  ]

  @descr_user_third = [
    'Belongs to Group3',
    'Can review changes made by Group2 users',
    'Can view comments left by Group2 and Group3 users. Can edit comments left by the Group2 users',
    'Can see the information about Group2 users',
    'The file favorite state is undefined',
    'Can’t copy data from the file to clipboard',
    'Can’t download the file',
    'Can’t print the file',
    'Can create new files from the editor',
    'Can’t close history',
    'Can’t restore the file version',
    'Can’t submit forms'
  ]

  @descr_user_null = [
    'The name is requested when the editor is opened',
    'Doesn’t belong to any group',
    'Can review all the changes',
    'Can perform all actions with comments',
    'Can’t see anyone’s information',
    'The file favorite state is undefined',
    "Can't mention others in comments",
    "Can't create new files from the editor",
    "Can't rename files from the editor",
    "Can't view chat",
    "Can't protect file",
    'View file without collaboration',
    'Can’t refresh outdated file',
    'Can’t submit forms'
  ]

  @users = [
    User.new(
      'uid-1',
      'John Smith',
      'smith@example.com',
      '',
      nil,
      {},
      nil,
      false,
      [],
      @descr_user_first,
      true,
      true,
      { blank: false },
      { visible: false }
    ),
    User.new(
      'uid-2',
      'Mark Pottato',
      'pottato@example.com',
      'group-2',
      ['group-2', ''],
      {
        view: '',
        edit: ['group-2', ''],
        remove: ['group-2']
      },
      ['group-2', ''],
      true,
      [],
      @descr_user_second,
      false,
      true,
      { text: 'Go to Documents' },
      {}
    ),
    User.new(
      'uid-3',
      'Hamish Mitchell',
      nil,
      'group-3',
      ['group-2'],
      {
        view: ['group-3', 'group-2'],
        edit: ['group-2'],
        remove: []
      },
      ['group-2'],
      nil,
      ['copy', 'download', 'print'],
      @descr_user_third,
      false,
      false,
      nil,
      {}
    ),
    User.new(
      'uid-0',
      nil,
      nil,
      '',
      nil,
      {},
      [],
      nil,
      ['protect'],
      @descr_user_null,
      false,
      false,
      nil,
      nil
    )
  ]

  def self.all_users
    @users
  end

  # get a user by id specified
  def self.get_user(id)
    @users.each do |user|
      return user if user.id.eql?(id)
    end
    @users[0]
  end

  # get a list of users with their names and emails for mentions
  def self.get_users_for_mentions(id)
    users_data = []
    @users.each do |user|
      if !user.id.eql?(id) && !user.name.nil? && !user.email.nil?
        users_data.push({ name: user.name, email: user.email })
      end
    end
    users_data
  end

  # get a list of users with their id, names and emails for protect
  def self.get_users_for_protect(id)
    users_data = []
    @users.each do |user|
      users_data.push({ id: user.id, name: user.name, email: user.email }) if !user.id.eql?(id) && !user.name.nil?
    end
    users_data
  end
end
