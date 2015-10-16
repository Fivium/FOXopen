/*
 Adapted from accessible-modal-dialog by gdkraus: https://github.com/gdkraus/accessible-modal-dialog

 ============================================
 License for Application
 ============================================

 This license is governed by United States copyright law, and with respect to matters
 of tort, contract, and other causes of action it is governed by North Carolina law,
 without regard to North Carolina choice of law provisions.  The forum for any dispute
 resolution shall be in Wake County, North Carolina.

 Redistribution and use in source and binary forms, with or without modification, are
 permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this list
 of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice, this
 list of conditions and the following disclaimer in the documentation and/or other
 materials provided with the distribution.

 3. The name of the author may not be used to endorse or promote products derived from
 this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR "AS IS" AND ANY EXPRESS OR IMPLIED
 WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

AccessibleModal = {
  // jQuery formatted selector to search for focusable items
  focusableElementsString: "a[href], area[href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), button:not([disabled]), iframe, object, embed, *[tabindex], *[contenteditable]",

  /**
   * Prevents tab key presses from focusing on elements outside the given object.
   * @param obj {jQuery} Object to prevent tabbing out of (i.e. the modal container)
   * @param evt {event} Keypress event which has fired.
   */
  trapTabKey: function (obj, evt) {
    // get list of all children elements in given object
    var o = obj.find('*');

    // get list of focusable items
    var focusableItems = o.filter(this.focusableElementsString).filter(':visible');

    // get currently focused item
    var focusedItem = jQuery(':focus');

    // get the number of focusable items
    var numberOfFocusableItems = focusableItems.length;

    // get the index of the currently focused item
    var focusedItemIndex = focusableItems.index(focusedItem);

    if (focusedItemIndex == -1 || (!evt.shiftKey && focusedItemIndex == numberOfFocusableItems - 1)) {
      //If focused outside the modal, bring focus to first item
      //Or if tab is pressed without shift, and we are on the last item, wrap around to the first item
      focusableItems.get(0).focus();
      evt.preventDefault();
    }
    else if (evt.shiftKey && focusedItemIndex == 0) {
      //back tab and we're focused on first item - go to the last focusable item
      focusableItems.get(numberOfFocusableItems - 1).focus();
      evt.preventDefault();
    }
  }
};
