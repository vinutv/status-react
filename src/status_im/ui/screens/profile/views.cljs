(ns status-im.ui.screens.profile.views
  (:require [clojure.string :as string]
            [re-frame.core :refer [dispatch]]
            [status-im.ui.components.action-button.action-button
             :refer
             [action-button action-button-disabled action-separator]]
            [status-im.ui.components.action-button.styles :refer [actions-list]]
            [status-im.ui.components.chat-icon.screen :refer [my-profile-icon]]
            [status-im.ui.components.common.common :as common]
            [status-im.ui.components.context-menu :refer [context-menu]]
            [status-im.ui.components.list-selection :refer [share-options]]
            [status-im.ui.components.react :as react]
            [status-im.ui.components.icons.vector-icons :as vector-icons]
            [status-im.ui.components.status-bar.view :refer [status-bar]]
            [status-im.ui.components.styles :refer [color-blue] :as component.styles]
            [status-im.ui.components.toolbar.actions :as actions]
            [status-im.ui.components.toolbar.view :as toolbar]
            [status-im.i18n :refer [label]]
            [status-im.ui.screens.profile.styles :as styles]
            [status-im.utils.utils :as utils]
            [status-im.utils.datetime :as time]
            [status-im.utils.utils :refer [hash-tag?]]
            [status-im.utils.config :as config]
            [status-im.utils.platform :as platform]
            [status-im.protocol.core :as protocol]
            [re-frame.core :as re-frame])
  (:require-macros [status-im.utils.views :refer [defview letsubs]]))

(defn my-profile-toolbar []
  [toolbar/toolbar {}
   nil
   [toolbar/content-title ""]
   [react/touchable-highlight
    {:on-press #(dispatch [:my-profile/edit-profile])}
    [react/view
     [react/text {:style      styles/toolbar-edit-text
                  :uppercase? component.styles/uppercase?} (label :t/edit)]]]])

(defn profile-toolbar [contact]
  [toolbar/toolbar {}
   toolbar/default-nav-back
   [toolbar/content-title ""]
   [toolbar/actions
    (when (and (not (:pending? contact))
               (not (:unremovable? contact)))
      [(actions/opts [{:value #(dispatch [:hide-contact contact])
                       :text  (label :t/remove-from-contacts)}])])]])

(defn online-text [last-online]
  (let [last-online-date (time/to-date last-online)
        now-date         (time/now)]
    (if (and (pos? last-online)
             (<= last-online-date now-date))
      (time/time-ago last-online-date)
      (label :t/active-unknown))))

(defn profile-badge [{:keys [name last-online] :as contact}]
  [react/view styles/profile-badge
   [my-profile-icon {:account contact
                     :edit?   false}]
   [react/view styles/profile-badge-name-container
    [react/text {:style           styles/profile-name-text
                 :number-of-lines 1}
     name]
    (when-not (nil? last-online)
      [react/view styles/profile-activity-status-container
       [react/text {:style styles/profile-activity-status-text}
        (online-text last-online)]])]])

(defn profile-actions [{:keys [pending? whisper-identity dapp?]} chat-id]
  [react/view actions-list
   (if pending?
     [action-button {:label     (label :t/add-to-contacts)
                     :icon      :icons/add
                     :icon-opts {:color :blue}
                     :on-press  #(dispatch [:add-pending-contact chat-id])}]
     [action-button-disabled {:label (label :t/in-contacts) :icon :icons/ok}])
   [action-separator]
   [action-button {:label     (label :t/start-conversation)
                   :icon      :icons/chats
                   :icon-opts {:color :blue}
                   :on-press  #(dispatch [:profile/send-message whisper-identity])}]
   (when-not dapp?
     [react/view
      [action-separator]
      [action-button {:label     (label :t/send-transaction)
                      :icon      :icons/arrow-right
                      :icon-opts {:color :blue}
                      :on-press  #(dispatch [:profile/send-transaction chat-id whisper-identity])}]])])

(defn profile-info-item [{:keys [label value options text-mode empty-value? accessibility-label]}]
  [react/view styles/profile-setting-item
   [react/view (styles/profile-info-text-container options)
    [react/text {:style styles/profile-settings-title}
     label]
    [react/view styles/profile-setting-spacing]
    [react/text {:style               (if empty-value?
                                        styles/profile-setting-text-empty
                                        styles/profile-setting-text)
                 :number-of-lines     1
                 :ellipsizeMode       text-mode
                 :accessibility-label accessibility-label}
     value]]
   (when options
     [context-menu
      [vector-icons/icon :icons/options]
      options
      nil
      styles/profile-info-item-button])])

(defn show-qr [contact qr-source qr-value]
  #(dispatch [:navigate-to-modal :qr-code-view {:contact   contact
                                                :qr-source qr-source
                                                :qr-value  qr-value}]))

(defn profile-options [contact k text]
  (into []
        (concat [{:value (show-qr contact k text)
                  :text  (label :t/show-qr)}]
                (when text
                  (share-options text)))))

(defn profile-info-address-item [{:keys [address] :as contact}]
  [profile-info-item
   {:label               (label :t/address)
    :value               address
    :options             (profile-options contact :address address)
    :text-mode           :middle
    :accessibility-label :profile-address}])

(defn profile-info-public-key-item [public-key contact]
  [profile-info-item
   {:label               (label :t/public-key)
    :value               public-key
    :options             (profile-options contact :public-key public-key)
    :text-mode           :middle
    :accessibility-label :profile-public-key}])

(defn settings-item-separator []
  [common/separator styles/settings-item-separator])

(defn tag-view [tag]
  [react/text {:style {:color color-blue}
               :font  :medium}
   (str tag " ")])

(defn colorize-status-hashtags [status]
  (for [[i status] (map-indexed vector (string/split status #" "))]
    (if (hash-tag? status)
      ^{:key (str "item-" i)}
      [tag-view status]
      ^{:key (str "item-" i)}
      (str status " "))))

(defn profile-info-phone-item [phone & [options]]
  (let [phone-empty? (or (nil? phone) (string/blank? phone))
        phone-text  (if phone-empty?
                      (label :t/not-specified)
                      phone)]
    [profile-info-item {:label               (label :t/phone-number)
                        :value               phone-text
                        :options             options
                        :empty-value?        phone-empty?
                        :accessibility-label :profile-phone-number}]))

(defn settings-title [title]
  [react/text {:style styles/profile-settings-title}
   title])

(defn settings-item [label-kw value action-fn active?]
  [react/touchable-highlight
   {:on-press action-fn
    :disabled (not active?)}
   [react/view styles/settings-item
    [react/text {:style styles/settings-item-text}
     (label label-kw)]
    [react/text {:style      styles/settings-item-value
                 :uppercase? component.styles/uppercase?} value]
    (when active?
      [vector-icons/icon :icons/forward {:color :gray}])]])

(defn profile-info [{:keys [whisper-identity phone] :as contact}]
  [react/view
   [profile-info-address-item contact]
   [settings-item-separator]
   [profile-info-public-key-item whisper-identity contact]
   [settings-item-separator]
   [profile-info-phone-item phone]])

(defn navigate-to-accounts []
  ;; TODO(rasom): probably not the best place for this call
  (protocol/stop-whisper!)
  (re-frame/dispatch [:navigate-to :accounts]))

(defn handle-logout []
  (utils/show-confirmation (label :t/logout-title)
                           (label :t/logout-are-you-sure)
                           (label :t/logout) navigate-to-accounts))

(defn logout []
  [react/view {}
   [react/touchable-highlight
    {:on-press handle-logout}
    [react/view styles/settings-item
     [react/text {:style      styles/logout-text
                  :font       (if platform/android? :medium :default)}
      (label :t/logout)]]]])

(defn my-profile-settings [{:keys [network networks] :as contact}]
  [react/view
   [settings-title (label :t/settings)]
   [settings-item :t/main-currency "USD" #() false]
   [settings-item-separator]
   [settings-item :t/notifications "" #() true]
   [settings-item-separator]
   [settings-item :t/network-settings (get-in networks [network :name]) #(dispatch [:navigate-to :network-settings]) true]
   (when config/offline-inbox-enabled?
     [settings-item-separator])
   (when config/offline-inbox-enabled?
     [settings-item :t/offline-messaging-settings "" #(dispatch [:navigate-to :offline-messaging-settings]) true])])

(defn profile-status [status & [edit?]]
  [react/view styles/profile-status-container
   (if (or (nil? status) (string/blank? status))
     [react/touchable-highlight {:on-press #(dispatch [:my-profile/edit-profile :edit-status])}
      [react/view
       [react/text {:style styles/add-a-status}
        (label :t/add-a-status)]]]
     [react/scroll-view
      [react/touchable-highlight {:on-press (when edit? #(dispatch [:my-profile/edit-profile :edit-status]))}
       [react/view
        [react/text {:style styles/profile-status-text}
         (colorize-status-hashtags status)]]]])])

(defn network-info []
  [react/view styles/network-info
   [common/network-info]
   [common/separator]])

(defn share-contact-code [current-account public-key]
  [react/touchable-highlight {:on-press (show-qr current-account :public-key public-key)}
   [react/view styles/share-contact-code
    [react/view styles/share-contact-code-text-container
     [react/text {:style styles/share-contact-code-text}
      (label :t/share-contact-code)]]
    [react/view styles/share-contact-icon-container
     [vector-icons/icon :icons/qr {:color component.styles/color-blue4}]]]])

(defview my-profile []
  (letsubs [{:keys [status public-key] :as current-account} [:get-current-account]]
    [react/view styles/profile
     [my-profile-toolbar]
     [react/scroll-view
      [react/view styles/profile-form
       [profile-badge current-account]]
      [react/view actions-list
       [share-contact-code current-account public-key]]
      [react/view styles/profile-info-container
       [my-profile-settings current-account]]
      [logout]]]))

(defview profile []
  (letsubs [{:keys [pending?
                    status
                    whisper-identity]
             :as   contact} [:contact]
            chat-id  [:get :current-chat-id]]
    [react/view styles/profile
     [status-bar]
     [profile-toolbar contact]
     [network-info]
     [react/scroll-view
      [react/view styles/profile-form
       [profile-badge contact]
       (when (and (not (nil? status)) (not (string/blank? status)))
         [profile-status status])]
      [common/form-spacer]
      [profile-actions contact chat-id]
      [common/form-spacer]
      [react/view styles/profile-info-container
       [profile-info contact]
       [common/bottom-shadow]]]]))
