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

package com.onlyoffice.integration.sdk.service;

import com.google.gson.Gson;
import com.onlyoffice.integration.documentserver.models.enums.Action;
import com.onlyoffice.integration.documentserver.storage.FileStoragePathBuilder;
import com.onlyoffice.integration.entities.Group;
import com.onlyoffice.integration.entities.Permission;
import com.onlyoffice.integration.sdk.manager.UrlManager;
import com.onlyoffice.integration.services.UserServices;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.common.User;
import com.onlyoffice.model.documenteditor.Config;
import com.onlyoffice.model.documenteditor.config.document.Info;
import com.onlyoffice.model.documenteditor.config.document.Permissions;
import com.onlyoffice.model.documenteditor.config.document.ReferenceData;
import com.onlyoffice.model.documenteditor.config.document.Type;
import com.onlyoffice.model.documenteditor.config.document.permissions.CommentGroups;
import com.onlyoffice.model.documenteditor.config.editorconfig.CoEditing;
import com.onlyoffice.model.documenteditor.config.editorconfig.Customization;
import com.onlyoffice.model.documenteditor.config.editorconfig.Embedded;
import com.onlyoffice.model.documenteditor.config.editorconfig.Mode;
import com.onlyoffice.model.documenteditor.config.editorconfig.Template;
import com.onlyoffice.model.documenteditor.config.editorconfig.customization.Goback;
import com.onlyoffice.model.documenteditor.config.editorconfig.customization.Close;
import com.onlyoffice.model.documenteditor.config.editorconfig.embedded.Toolbar;
import com.onlyoffice.service.documenteditor.config.DefaultConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class ConfigServiceImpl extends DefaultConfigService implements ConfigService {

    @Autowired
    private UserServices userService;

    @Autowired
    private FileStoragePathBuilder storagePathBuilder;

    @Autowired
    private UrlManager urlManager;


    public ConfigServiceImpl(final DocumentManager documentManager, final UrlManager argUrlManager,
                             final JwtManager jwtManager, final SettingsManager settingsManager) {
        super(documentManager, argUrlManager, jwtManager, settingsManager);
    }

    @Override
    public Config createConfig(final String fileId, final Action action, final Type type) {
        com.onlyoffice.integration.entities.User appUser = userService.getCurrentUser();
        Action currentAction = action;
        String fileName = getDocumentManager().getDocumentName(fileId);
        if (currentAction == null) {
            currentAction = Action.edit;
        }
        Boolean isEditable = getDocumentManager().isEditable(fileName);
        if ((!isEditable && currentAction.equals(Action.edit) || currentAction.equals(Action.fillForms))
            && getDocumentManager().isFillable(fileName)) {
            isEditable = true;
            currentAction = Action.fillForms;
        }

        Mode mode = isEditable && !currentAction.equals(Action.view) ? Mode.EDIT : Mode.VIEW;

        Config config = super.createConfig(fileId, mode, type);

        if (!currentAction.equals(Action.view)
                && appUser.getPermissions().getSubmitForm()) {
            config.getEditorConfig().getCustomization().setSubmitForm(true);
        }

        Permissions permissions = config.getDocument().getPermissions();
        permissions = updatePermissions(permissions, currentAction, isEditable);

        config.getDocument().setPermissions(permissions);

        if (appUser.getName().equals("Anonymous")) {
            config.getEditorConfig().setCreateUrl(null);
        }

        if (getSettingsManager().isSecurityEnabled()) {
            config.setToken(getJwtManager().createToken(config));
        }

        return config;
    }

    @Override
    public ReferenceData getReferenceData(final String fileId) {
        Gson gson = new Gson();

        HashMap<String, String> fileKey = new HashMap<>();
        fileKey.put("fileName", getDocumentManager().getDocumentName(fileId));
        try {
            fileKey.put("userAddress", InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
        ReferenceData referenceData = ReferenceData.builder()
                .instanceId(storagePathBuilder.getServerUrl(true))
                .fileKey(gson.toJson(fileKey))
                .build();

        return referenceData;
    }

    @Override
    public Info getInfo(final String fileId) {
        com.onlyoffice.integration.entities.User appUser = userService.getCurrentUser();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM dd yyyy", Locale.US);

        return Info.builder()
                .owner("Me")
                .favorite(appUser.getFavorite())
                .uploaded(simpleDateFormat.format(new Date(
                        new File(storagePathBuilder.getFileLocation(fileId)).lastModified())
                ))
                .build();
    }

    @Override
    public Permissions getPermissions(final String fileId) {
        com.onlyoffice.integration.entities.User appUser = userService.getCurrentUser();

        if (appUser == null) {
            return null;
        }

        Permission userPermissions = appUser.getPermissions();

        return Permissions.builder()
                .chat(userPermissions.getChat())
                .comment(userPermissions.getComment())
                .commentGroups(getCommentGroups(userPermissions))
                .copy(userPermissions.getCopy())
                .download(userPermissions.getDownload())
                .edit(userPermissions.getEdit())
                .fillForms(userPermissions.getFillForms())
                .modifyContentControl(userPermissions.getModifyContentControl())
                .modifyFilter(userPermissions.getModifyFilter())
                .print(userPermissions.getPrint())
                .protect(userPermissions.getProtect())
                .review(userPermissions.getReview())
                .reviewGroups(getReviewGroups(userPermissions))
                .userInfoGroups(getUserInfoGroups(userPermissions))
                .build();
    }

    @Override
    public List<Template> getTemplates(final String fileId) {
        com.onlyoffice.integration.entities.User appUser = userService.getCurrentUser();

        if (!appUser.getName().equals("Anonymous")) {

            String fileName = getDocumentManager().getDocumentName(fileId);

            return List.of(
                    Template.builder()
                            .image("")
                            .title("Blank")
                            .url(getUrlManager().getCreateUrl(fileName))
                            .build(), // create a blank template
                    Template.builder()
                            .image(urlManager.getTemplateImageUrl(fileName))
                            .title("With sample content")
                            .url(urlManager.getCreateSampleUrl(fileName))
                            .build()// create a template with sample content using the template image
            );
        }

        return null;
    }

    @Override
    public User getUser() {
        com.onlyoffice.integration.entities.User appUser = userService.getCurrentUser();

        if (appUser == null) {
            return null;
        }

        User user = User.builder()
                .id(String.valueOf(appUser.getId()))
                .name(appUser.getName())
                .group(appUser.getGroup().getName())
                .build();

        if (appUser.getAvatar()) {
            user.setImage(storagePathBuilder.getServerUrl(true)
                    + "/css/img/uid-"
                    + appUser.getId()
                    + ".png"
            );
        }

        return user;
    }

    @Override
    public Customization getCustomization(final String fileId) {
        com.onlyoffice.integration.entities.User appUser = userService.getCurrentUser();

        Goback goback = Goback.builder()
                .url(getUrlManager().getGobackUrl(fileId))
                .build();


        if (appUser != null && appUser.getGoback() != null) {
            goback.setText(appUser.getGoback().getText());
            goback.setBlank(appUser.getGoback().getBlank());
        } else {
            goback.setUrl("");
        }

        Close close = Close.builder()
            .build();

        if (appUser != null && appUser.getClose() != null) {
            close.setText(appUser.getClose().getText());
            close.setVisible(appUser.getClose().getVisible());
        }

        Customization customization = Customization.builder()
                .autosave(true) // if the Autosave menu option is enabled or disabled
                .comments(true) // if the Comments menu button is displayed or hidden
                .compactHeader(false) /* if the additional action buttons are displayed
    in the upper part of the editor window header next to the logo (false) or in the toolbar (true) */
                .compactToolbar(false) // if the top toolbar type displayed is full (false) or compact (true)
                .forcesave(false)/* add the request for the forced file saving to the callback handler
    when saving the document within the document editing service */
                .help(true)  //  if the Help menu button is displayed or hidden
                .hideRightMenu(false) // if the right menu is displayed or hidden on first loading
                .hideRulers(false) // if the editor rulers are displayed or hidden
                .feedback(true)
                .goback(goback)
                .close(close)
                .build();

        return customization;
    }

    @Override
    public CoEditing getCoEditing(final String fileId, final Mode mode, final Type type) {
        com.onlyoffice.integration.entities.User appUser = userService.getCurrentUser();

        if (mode.equals(Mode.VIEW) && appUser.getName().equals("Anonymous")) {
            return CoEditing.builder()
                    .mode(com.onlyoffice.model.documenteditor.config.editorconfig.coediting.Mode.STRICT)
                    .change(false)
                    .build();
        }

        return null;
    }

    @Override
    public Embedded getEmbedded(final String fileId) {
        String url = getUrlManager().getFileUrl(fileId);

        return Embedded.builder()
                   .embedUrl(url)
                   .saveUrl(url)
                   .shareUrl(url)
                   .toolbarDocked(Toolbar.TOP)
                   .build();
    }

    private CommentGroups getCommentGroups(final Permission userPermissions) {
        CommentGroups commentGroups = new CommentGroups();

        List<String> edit = userPermissions.getCommentsEditGroups().stream()
                .filter(commentGroup -> !commentGroup.getName().equals("NULL"))
                .map(Group::getName)
                .collect(Collectors.toList());

        if (edit != null && edit.size() > 0) {
            commentGroups.setEdit(edit);
        }

        List<String> view = userPermissions.getCommentsViewGroups().stream()
                .filter(commentGroup -> !commentGroup.getName().equals("NULL"))
                .map(Group::getName)
                .collect(Collectors.toList());

        if (view != null && view.size() > 0) {
            commentGroups.setView(view
            );
        }

        List<String> remove = userPermissions.getCommentsRemoveGroups().stream()
                .filter(commentGroup -> !commentGroup.getName().equals("NULL"))
                .map(Group::getName)
                .collect(Collectors.toList());

        if (remove != null && remove.size() > 0) {
            commentGroups.setRemove(remove);
        }

        return commentGroups;
    }

    private List<String> getReviewGroups(final Permission userPermissions) {
        List<String> reviewGroups = new ArrayList<>();

        if (userPermissions.getReviewGroups() != null) {
            reviewGroups = userPermissions.getReviewGroups().stream()
                    .filter(commentGroup -> !commentGroup.getName().equals("NULL"))
                    .map(Group::getName)
                    .collect(Collectors.toList());
        }

        return reviewGroups != null && reviewGroups.size() > 0 ? reviewGroups : null;
    }

    private List<String> getUserInfoGroups(final Permission userPermissions) {
        List<String> userInfoGroups = new ArrayList<>();

        if (userPermissions.getUserInfoGroups() != null) {
            userInfoGroups = userPermissions.getUserInfoGroups().stream()
                    .filter(commentGroup -> !commentGroup.getName().equals("NULL"))
                    .map(Group::getName)
                    .collect(Collectors.toList());
        }

        return userInfoGroups != null && userInfoGroups.size() > 0 ? userInfoGroups : null;
    }

    private Permissions updatePermissions(final Permissions permissions, final Action action,
                                          final Boolean isEditable) {
        permissions.setComment(
                !action.equals(Action.view)
                        && !action.equals(Action.fillForms)
                        && !action.equals(Action.embedded)
                        && !action.equals(Action.blockcontent)
        );

        permissions.setFillForms(
                !action.equals(Action.view)
                        && !action.equals(Action.comment)
                        && !action.equals(Action.blockcontent)
        );

        permissions.setReview(isEditable
                && (action.equals(Action.review) || action.equals(Action.edit)));

        permissions.setEdit(isEditable
                && (action.equals(Action.view)
                || action.equals(Action.edit)
                || action.equals(Action.filter)
                || action.equals(Action.blockcontent)));

        return permissions;
    }
}
