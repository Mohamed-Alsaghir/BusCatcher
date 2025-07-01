document.addEventListener('DOMContentLoaded', function () {
    var listItems = document.querySelectorAll('.list-group-item');

    listItems.forEach(function (item) {
      item.addEventListener('click', function () {
        listItems.forEach(function (otherItem) {
          otherItem.classList.remove('selected');
        });
        this.classList.toggle('selected');
      });
    });
    
  
  });